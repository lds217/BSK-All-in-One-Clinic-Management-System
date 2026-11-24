<#
.SYNOPSIS
    Updates the BSK client or server from the latest GitHub release, then starts it.

.DESCRIPTION
    Reads bsk-manifest.json from the latest release. The manifest lists every file of the
    installation (bsk.jar, client.jar, server.jar, lib\*.jar, this script) with its SHA-256 hash.
    Only files whose hash differs from the local copy are downloaded, each download is verified,
    and files are only replaced once everything has been downloaded. The replaced files are kept
    in update_backup\ so -Rollback can restore them.

    If anything goes wrong (no internet, bad download, files in use) the current version is
    started instead, so the clinic can always work.

    Usually run through start_client.bat / start_server.bat (Mode start) or
    update_client.bat / update_server.bat (Mode update).

.PARAMETER App
    client or server: which program to start afterwards.

.PARAMETER Mode
    start  - update only if the program is not running, then start it (daily use).
    update - offer to close the running program(s), update, then start (for the administrator).

.PARAMETER Rollback
    Restore the files saved by the last update, then start the program.

.PARAMETER NoLaunch
    Do not start the program afterwards.
#>
param(
    [Parameter(Mandatory = $true)][ValidateSet('client', 'server')][string]$App,
    [ValidateSet('start', 'update')][string]$Mode = 'start',
    [switch]$Rollback,
    [switch]$NoLaunch,
    [string]$ManifestUrl = 'https://github.com/lds217/BSK-All-in-One-Clinic-Management-System/releases/latest/download/bsk-manifest.json'
)

$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'   # the progress bar makes downloads very slow in PowerShell 5.1
[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
if ($env:BSK_MANIFEST_URL) { $ManifestUrl = $env:BSK_MANIFEST_URL }

$Root = $PSScriptRoot
Set-Location $Root
$StagingDir = Join-Path $Root 'update_tmp'
$BackupDir = Join-Path $Root 'update_backup'
$LogDir = Join-Path $Root 'logs'
$LogFile = Join-Path $LogDir 'update.log'
$AppJar = "$App.jar"
$VersionFiles = @('version_client.json', 'version_server.json')

# ---------------------------------------------------------------------------------------------
function Write-Log([string]$Message, [string]$Color = 'Gray') {
    Write-Host $Message -ForegroundColor $Color
    try {
        if (-not (Test-Path $LogDir)) { New-Item -ItemType Directory -Path $LogDir | Out-Null }
        Add-Content -Path $LogFile -Value ("[{0}] [{1}] {2}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $App, $Message)
    } catch { }
}

function Get-Sha256([string]$Path) {
    return (Get-FileHash -Algorithm SHA256 -LiteralPath $Path).Hash.ToLowerInvariant()
}

function Get-InstalledVersion {
    $path = Join-Path $Root "version_$App.json"
    if (-not (Test-Path $path)) { return $null }
    try { return (Get-Content -Raw -LiteralPath $path | ConvertFrom-Json).currentVersion } catch { return $null }
}

function Set-InstalledVersion([string]$Version) {
    $json = "{`r`n    `"currentVersion`": `"$Version`"`r`n}"
    foreach ($name in $VersionFiles) {
        # UTF-8 without BOM: the app parses this file as JSON.
        [IO.File]::WriteAllText((Join-Path $Root $name), $json)
    }
}

function Get-RunningApps {
    # Java processes started from client.jar or server.jar (both share bsk.jar and lib\).
    Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" |
        Where-Object { $_.CommandLine -match '(?i)(client|server)\.jar' }
}

function Test-FileUnlocked([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return $true }
    try {
        $stream = [IO.File]::Open($Path, 'Open', 'ReadWrite', 'None')
        $stream.Close()
        return $true
    } catch { return $false }
}

function Resolve-InstallPath([string]$RelativePath) {
    # Only plain relative paths inside the install folder are accepted from the manifest.
    if ($RelativePath -notmatch '^[A-Za-z0-9._\-]+(/[A-Za-z0-9._\-]+)*$' -or $RelativePath -match '(^|/)\.\.(/|$)') {
        throw "Invalid path in manifest: $RelativePath"
    }
    return Join-Path $Root ($RelativePath -replace '/', '\')
}

function Move-IntoPlace([string]$Source, [string]$Destination) {
    $dir = Split-Path -Parent $Destination
    if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
    Move-Item -LiteralPath $Source -Destination $Destination -Force
}

function Start-App {
    if ($NoLaunch) { return }
    $jar = Join-Path $Root $AppJar
    if (-not (Test-Path $jar)) { Write-Log "Không tìm thấy $AppJar." 'Red'; exit 1 }
    Write-Log "Đang mở $AppJar..." 'Green'
    try {
        # Same as double-clicking the jar: uses the installed Java.
        Start-Process -FilePath $jar -WorkingDirectory $Root
    } catch {
        Start-Process -FilePath 'javaw' -ArgumentList @('-jar', "`"$jar`"") -WorkingDirectory $Root
    }
}

# ---------------------------------------------------------------------------------------------
function Invoke-Rollback {
    if (-not (Test-Path $BackupDir)) { Write-Log 'Không có bản sao lưu để khôi phục.' 'Yellow'; return }
    $saved = Get-ChildItem -LiteralPath $BackupDir -Recurse -File | Where-Object { $_.Name -ne 'previous_version.txt' }
    foreach ($file in $saved) {
        $target = Join-Path $Root $file.FullName.Substring($BackupDir.Length + 1)
        if (-not (Test-FileUnlocked $target)) { throw "File đang được sử dụng: $target. Hãy đóng chương trình rồi thử lại." }
    }
    foreach ($file in $saved) {
        $target = Join-Path $Root $file.FullName.Substring($BackupDir.Length + 1)
        Copy-Item -LiteralPath $file.FullName -Destination $target -Force
    }
    $versionFile = Join-Path $BackupDir 'previous_version.txt'
    if (Test-Path $versionFile) { Set-InstalledVersion (Get-Content -Raw $versionFile).Trim() }
    Write-Log "Đã khôi phục $($saved.Count) file từ bản sao lưu." 'Green'
}

function Invoke-Update {
    Write-Log "Đang kiểm tra bản cập nhật..."
    try {
        # GitHub serves release files as application/octet-stream, so parse the JSON ourselves.
        $response = Invoke-WebRequest -Uri $ManifestUrl -TimeoutSec 20 -UseBasicParsing
        $text = if ($response.Content -is [byte[]]) { [Text.Encoding]::UTF8.GetString($response.Content) } else { [string]$response.Content }
        $manifest = $text.TrimStart([char]0xFEFF) | ConvertFrom-Json
    } catch {
        Write-Log "Không kiểm tra được bản cập nhật (không có mạng?). Dùng phiên bản hiện tại. $($_.Exception.Message)" 'Yellow'
        return
    }
    if (-not $manifest.version -or -not $manifest.files) { Write-Log 'File cập nhật không hợp lệ, bỏ qua.' 'Yellow'; return }

    $installed = Get-InstalledVersion
    $entries = @($manifest.files | ForEach-Object {
        [pscustomobject]@{ Path = $_.path; Local = (Resolve-InstallPath $_.path); Sha = $_.sha256.ToLowerInvariant(); Size = [int64]$_.size; Url = $_.url }
    })

    if ($installed -eq $manifest.version) {
        $missing = @($entries | Where-Object { -not (Test-Path -LiteralPath $_.Local) -or (Get-Item -LiteralPath $_.Local).Length -ne $_.Size })
        if ($missing.Count -eq 0) { Write-Log "Đang dùng phiên bản mới nhất ($installed)." 'Green'; return }
    }

    $changed = @($entries | Where-Object { -not (Test-Path -LiteralPath $_.Local) -or (Get-Sha256 $_.Local) -ne $_.Sha })
    $manifestLibs = @($entries | Where-Object { $_.Path -like 'lib/*' } | ForEach-Object { Split-Path -Leaf $_.Local })
    $stale = @()
    if (Test-Path (Join-Path $Root 'lib')) {
        $stale = @(Get-ChildItem -LiteralPath (Join-Path $Root 'lib') -Filter '*.jar' -File | Where-Object { $manifestLibs -notcontains $_.Name })
    }

    if ($changed.Count -eq 0 -and $stale.Count -eq 0) {
        Set-InstalledVersion $manifest.version
        Write-Log "Đang dùng phiên bản mới nhất ($($manifest.version))." 'Green'
        return
    }

    $totalMb = [math]::Round((($changed | Measure-Object -Property Size -Sum).Sum) / 1MB, 1)
    Write-Log "Có phiên bản mới: $($manifest.version) (đang dùng: $installed). Cần tải $($changed.Count) file, $totalMb MB." 'Cyan'
    if ($manifest.releaseNotes) { Write-Log "Ghi chú: $($manifest.releaseNotes)" }

    $locked = @(($changed + $stale) | ForEach-Object { if ($_.Local) { $_.Local } else { $_.FullName } } | Where-Object { -not (Test-FileUnlocked $_) })
    if ($locked.Count -gt 0) {
        Write-Log "Chương trình đang chạy nên chưa thể cập nhật ($(Split-Path -Leaf $locked[0]) đang được dùng). Dùng phiên bản hiện tại." 'Yellow'
        return
    }

    # 1. Download everything to update_tmp\ and verify it. Nothing is replaced yet.
    if (Test-Path $StagingDir) { Remove-Item -LiteralPath $StagingDir -Recurse -Force }
    New-Item -ItemType Directory -Path $StagingDir | Out-Null
    $client = New-Object System.Net.WebClient
    try {
        $i = 0
        foreach ($entry in $changed) {
            $i++
            $staged = Join-Path $StagingDir ($entry.Path -replace '/', '\')
            $dir = Split-Path -Parent $staged
            if (-not (Test-Path $dir)) { New-Item -ItemType Directory -Path $dir -Force | Out-Null }
            Write-Host ("  [{0}/{1}] {2} ({3} MB)" -f $i, $changed.Count, $entry.Path, [math]::Round($entry.Size / 1MB, 1))
            $client.DownloadFile($entry.Url, $staged)
            if ((Get-Item -LiteralPath $staged).Length -ne $entry.Size -or (Get-Sha256 $staged) -ne $entry.Sha) {
                throw "File tải về bị lỗi (sai mã kiểm tra): $($entry.Path)"
            }
        }
    } catch {
        Write-Log "Tải bản cập nhật thất bại, giữ nguyên phiên bản hiện tại. $($_.Exception.Message)" 'Red'
        Remove-Item -LiteralPath $StagingDir -Recurse -Force -ErrorAction SilentlyContinue
        return
    } finally {
        $client.Dispose()
    }

    # 2. Swap the files in, keeping the old ones in update_backup\. Undo everything on failure.
    if (Test-Path $BackupDir) { Remove-Item -LiteralPath $BackupDir -Recurse -Force }
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
    if ($installed) { Set-Content -LiteralPath (Join-Path $BackupDir 'previous_version.txt') -Value $installed }
    $done = New-Object System.Collections.Generic.List[object]
    try {
        foreach ($file in $stale) {
            $backup = Join-Path $BackupDir ('lib\' + $file.Name)
            Move-IntoPlace $file.FullName $backup
            $done.Add(@{ Target = $file.FullName; Backup = $backup })
        }
        foreach ($entry in $changed) {
            $relative = $entry.Path -replace '/', '\'
            $backup = $null
            if (Test-Path -LiteralPath $entry.Local) {
                $backup = Join-Path $BackupDir $relative
                Move-IntoPlace $entry.Local $backup
            }
            $done.Add(@{ Target = $entry.Local; Backup = $backup })
            Move-IntoPlace (Join-Path $StagingDir $relative) $entry.Local
        }
        Set-InstalledVersion $manifest.version
        Write-Log "Đã cập nhật lên phiên bản $($manifest.version)." 'Green'
    } catch {
        Write-Log "Cập nhật thất bại, đang khôi phục phiên bản cũ. $($_.Exception.Message)" 'Red'
        for ($j = $done.Count - 1; $j -ge 0; $j--) {
            $step = $done[$j]
            if (Test-Path -LiteralPath $step.Target) { Remove-Item -LiteralPath $step.Target -Force -ErrorAction SilentlyContinue }
            if ($step.Backup -and (Test-Path -LiteralPath $step.Backup)) { Move-IntoPlace $step.Backup $step.Target }
        }
    } finally {
        Remove-Item -LiteralPath $StagingDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

# ---------------------------------------------------------------------------------------------
Write-Log "===== BSK $App ($Mode$(if ($Rollback) { ', rollback' })) ====="
try {
    $running = @(Get-RunningApps)
    if ($running.Count -gt 0) {
        if ($Mode -eq 'update' -or $Rollback) {
            Write-Host ''
            Write-Host 'Các chương trình BSK đang chạy trên máy này:' -ForegroundColor Yellow
            $running | ForEach-Object { Write-Host "  PID $($_.ProcessId): $($_.CommandLine)" }
            $answer = Read-Host 'Đóng các chương trình này để cập nhật? (Y/N)'
            if ($answer -match '^[Yy]') {
                $running | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
                Start-Sleep -Seconds 3   # let Windows release the jar files
                $running = @()
            }
        } elseif ($App -eq 'server' -and @($running | Where-Object { $_.CommandLine -match '(?i)server\.jar' }).Count -gt 0) {
            Write-Log 'Máy chủ đang chạy rồi.' 'Yellow'
            exit 2
        }
    }

    if ($Rollback) {
        Invoke-Rollback
    } elseif ($running.Count -gt 0) {
        Write-Log 'Chương trình BSK đang chạy trên máy này nên bỏ qua cập nhật lần này.' 'Yellow'
    } else {
        Invoke-Update
    }
} catch {
    Write-Log "Lỗi: $($_.Exception.Message)" 'Red'
}

Start-App
exit 0
