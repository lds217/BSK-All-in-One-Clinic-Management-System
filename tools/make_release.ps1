<#
.SYNOPSIS
    Builds a release and writes target\release\bsk-manifest.json for bsk_update.ps1.

.DESCRIPTION
    1. Builds target\release (bsk.jar, client.jar, server.jar, lib\, bsk_update.ps1).
    2. Lists every file with its size and SHA-256.
       - lib\*.jar point at Maven Central, so dependency jars never need to be uploaded.
       - Everything else points at the GitHub release v<Version>.
    3. Prints the files to upload to that GitHub release.

    Clinic PCs read https://github.com/<Repo>/releases/latest/download/bsk-manifest.json, so the
    release must be published as the latest release (not a draft or pre-release).

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File tools\make_release.ps1 -Version 1.6.0 -Notes "Mã hoá kết nối"
#>
param(
    [Parameter(Mandatory = $true)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [string]$Notes = '',
    [string]$Repo = 'lds217/BSK-All-in-One-Clinic-Management-System',
    [switch]$SkipBuild,
    [switch]$SkipUrlCheck
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

$ProjectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectRoot
$ReleaseDir = Join-Path $ProjectRoot 'target\release'
$ManifestName = 'bsk-manifest.json'

if (-not $SkipBuild) {
    Write-Host 'Building (mvn clean package)...' -ForegroundColor Cyan
    & mvn -q clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
}
foreach ($required in @('bsk.jar', 'client.jar', 'server.jar', 'bsk_update.ps1', 'lib')) {
    if (-not (Test-Path (Join-Path $ReleaseDir $required))) { throw "Missing target\release\$required. Run without -SkipBuild." }
}

# Map each dependency file name to its Maven Central URL.
Write-Host 'Resolving dependency coordinates...' -ForegroundColor Cyan
$depsFile = Join-Path $ProjectRoot 'target\release-deps.txt'
& mvn -q dependency:list -DincludeScope=runtime "-DoutputFile=$depsFile"
if ($LASTEXITCODE -ne 0) { throw 'mvn dependency:list failed.' }
$central = @{}
foreach ($line in Get-Content $depsFile) {
    # group:artifact:type[:classifier]:version:scope [-- module ...]
    $coords = ($line -replace '\x1b\[[0-9;]*m', '' -replace '\s--\s.*$', '').Trim()
    $parts = $coords.Split(':')
    if ($parts.Count -lt 5) { continue }
    $group, $artifact, $type = $parts[0], $parts[1], $parts[2]
    if ($parts.Count -ge 6) { $classifier = $parts[3]; $ver = $parts[4] } else { $classifier = ''; $ver = $parts[3] }
    $fileName = if ($classifier) { "$artifact-$ver-$classifier.$type" } else { "$artifact-$ver.$type" }
    $central[$fileName] = 'https://repo1.maven.org/maven2/{0}/{1}/{2}/{3}' -f ($group -replace '\.', '/'), $artifact, $ver, $fileName
}

# Only the installation files are published; anything else in target\release is ignored.
$releaseFiles = @('bsk.jar', 'client.jar', 'server.jar', 'bsk_update.ps1' | ForEach-Object { Get-Item -LiteralPath (Join-Path $ReleaseDir $_) }) +
    @(Get-ChildItem -LiteralPath (Join-Path $ReleaseDir 'lib') -Filter '*.jar' -File | Sort-Object Name)
$files = @()
foreach ($file in $releaseFiles) {
    $relative = $file.FullName.Substring($ReleaseDir.Length + 1) -replace '\\', '/'
    if ($relative -like 'lib/*') {
        $url = $central[$file.Name]
        if (-not $url) { throw "No Maven coordinates found for $relative" }
    } else {
        $url = "https://github.com/$Repo/releases/download/v$Version/$relative"
    }
    $files += [pscustomobject][ordered]@{
        path   = $relative
        size   = $file.Length
        sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $file.FullName).Hash.ToLowerInvariant()
        url    = $url
    }
}

if (-not $SkipUrlCheck) {
    Write-Host "Checking that $(@($files | Where-Object { $_.path -like 'lib/*' }).Count) dependency URLs exist on Maven Central..." -ForegroundColor Cyan
    foreach ($entry in $files | Where-Object { $_.path -like 'lib/*' }) {
        try {
            $head = Invoke-WebRequest -Uri $entry.url -Method Head -UseBasicParsing -TimeoutSec 30
            if ([int64]$head.Headers['Content-Length'] -ne $entry.size) { throw "size differs from the local file" }
        } catch {
            throw "Dependency is not downloadable from Maven Central: $($entry.path) ($($entry.url)): $($_.Exception.Message)"
        }
    }
}

$manifest = [ordered]@{
    version      = $Version
    releaseNotes = $Notes
    files        = $files
}
$manifestPath = Join-Path $ReleaseDir $ManifestName
[IO.File]::WriteAllText($manifestPath, ($manifest | ConvertTo-Json -Depth 4))   # UTF-8 without BOM

$upload = @($files | Where-Object { $_.path -notlike 'lib/*' } | ForEach-Object { $_.path }) + $ManifestName
$libMb = [math]::Round((($files | Where-Object { $_.path -like 'lib/*' } | Measure-Object -Property size -Sum).Sum) / 1MB, 1)
Write-Host ''
Write-Host "Release $Version is ready in target\release" -ForegroundColor Green
Write-Host "  $($files.Count) files; $libMb MB of dependencies come from Maven Central."
Write-Host ''
Write-Host "Create the GitHub release with tag v$Version (published as Latest, not a pre-release) and upload:" -ForegroundColor Yellow
$upload | ForEach-Object { Write-Host "  target\release\$_" }
Write-Host ''
Write-Host 'Optional, only to move PCs from the old updater: also upload' -ForegroundColor Yellow
Write-Host '  target\release\start_client.bat, start_server.bat, update_client.bat, update_server.bat'
Write-Host ''
Write-Host 'With the GitHub CLI:' -ForegroundColor Yellow
Write-Host ("  gh release create v{0} --title v{0} --notes `"{1}`" {2}" -f $Version, $Notes, (($upload | ForEach-Object { "target/release/$_" }) -join ' '))
