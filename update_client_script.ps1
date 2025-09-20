<#
.SYNOPSIS
    Checks for a new version of the client JAR application, downloads it, and replaces the old version.

.DESCRIPTION
    This script is dedicated to updating the client application.
    It reads a local version_client.json file to determine the current version.
    It then fetches a remote latest_client.json file from a specified URL to see if a newer version is available.
    If an update is found, it stops the running client, downloads the new JAR,
    backs it up, installs the new one, and restarts the client.

.NOTES
    Author: Dat Le
    Date: 2025-09-07
    Version: 1.1.0 (Client Only)
#>

# --- CONFIGURATION ---
# IMPORTANT: This URL should now point to your 'latest_client.json' file on GitHub.
$remoteVersionInfoUrl = "https://github.com/lds217/BSK-All-in-One-Clinic-Management-System/releases/download/v1.1.0/latest.json"

# --- SCRIPT START ---
$ProgressPreference = 'Continue'

# Get the directory where this script is located.
$scriptPath = $PSScriptRoot
# Get the directory where this script is located.
$scriptPath = $PSScriptRoot

# Define the paths for the local files (now client-specific)
$localVersionFile = Join-Path $scriptPath "version_client.json"
$jarFileName = "client.jar"
$localJarFile = Join-Path $scriptPath $jarFileName
$backupJarFile = Join-Path $scriptPath "$($jarFileName).old"

# --- Step 1: Read the local version information ---
Write-Host "Reading local client version info from '$localVersionFile'..."
if (-not (Test-Path $localVersionFile)) {
    Write-Error "Error: Local version file not found at '$localVersionFile'. Cannot proceed."
    Read-Host "Press Enter to exit"
    exit 1
}

try {
    $localVersionInfo = Get-Content $localVersionFile | ConvertFrom-Json
    Write-Host " - Local Client Version: $($localVersionInfo.currentVersion)"
}
catch {
    Write-Error "Error: Could not read or parse the local version_client.json file. Is it valid JSON?"
    Read-Host "Press Enter to exit"
    exit 1
}


# --- Step 2: Fetch the remote version information ---
Write-Host "Fetching remote client version info from '$remoteVersionInfoUrl'..."
try {
    $remoteVersionInfo = Invoke-RestMethod -Uri $remoteVersionInfoUrl
    Write-Host " - Latest Remote Version: $($remoteVersionInfo.latestVersion)"
}
catch {
    Write-Error "Error: Failed to fetch or parse remote version info. Check the URL and your internet connection."
    Read-Host "Press Enter to exit"
    exit 1
}


# --- Step 3: Compare versions ---
if ([version]$localVersionInfo.currentVersion -ge [version]$remoteVersionInfo.latestVersion) {
    Write-Host "`nResult: Your client application is up-to-date! (Version $($localVersionInfo.currentVersion))"
}
else {
    Write-Host "`nResult: New version available! (Version $($remoteVersionInfo.latestVersion)). Starting update process."
    Write-Host "Release Notes: $($remoteVersionInfo.releaseNotes)"

    # Get the correct download URL (now simplified)
    $downloadUrl = $remoteVersionInfo.files.client
    if (-not $downloadUrl) {
        Write-Error "Error: 'downloadUrl' not found in remote JSON."
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-Host "Download URL: $downloadUrl"

    # --- Step 4: Stop the application ---
    Write-Host "`nStopping application..."
    try {
        # This command finds the java process running 'client.jar' and stops it.
        Get-CimInstance Win32_Process | Where-Object { $_.CommandLine -like "*$jarFileName*" } | ForEach-Object { Stop-Process -Id $_.ProcessId -Force }
        Write-Host " - Application stopped successfully."
        Start-Sleep -Seconds 3 # Give the OS time to release file locks
    }
    catch {
        Write-Warning "Could not automatically stop the application. Please stop it manually before continuing."
        Read-Host "Press Enter to continue after stopping the application"
    }


    # --- Step 5: Download and Replace ---
    $tempDownloadPath = Join-Path $scriptPath "client.jar.download"

    Write-Host "`nDownloading new version..."
    try {
        # First, get the file size for progress calculation
        $response = Invoke-WebRequest -Uri $downloadUrl -Method Head
        $totalSize = [int64]$response.Headers.'Content-Length'
        
        Write-Host "File size: $([math]::Round($totalSize/1MB, 2)) MB"
        Write-Host "Starting download with progress indicator..."
        
        # Use Start-BitsTransfer for better progress display if available
        if (Get-Command Start-BitsTransfer -ErrorAction SilentlyContinue) {
            Start-BitsTransfer -Source $downloadUrl -Destination $tempDownloadPath -DisplayName "Downloading client.jar" -Description "BSK Client Update"
        }
        else {
            # Fallback: Custom progress implementation
            $webClient = New-Object System.Net.WebClient
            
            # Register progress event
            Register-ObjectEvent -InputObject $webClient -EventName DownloadProgressChanged -Action {
                $percent = $Event.SourceEventArgs.ProgressPercentage
                $received = $Event.SourceEventArgs.BytesReceived
                $total = $Event.SourceEventArgs.TotalBytesToReceive
                $speed = [math]::Round($received / 1MB, 2)
                
                Write-Progress -Activity "Downloading client.jar" -Status "$percent% Complete" -PercentComplete $percent -CurrentOperation "Downloaded: $speed MB / $([math]::Round($total/1MB, 2)) MB"
            } | Out-Null
            
            try {
                $webClient.DownloadFile($downloadUrl, $tempDownloadPath)
                Write-Progress -Activity "Downloading client.jar" -Completed
            }
            finally {
                $webClient.Dispose()
                Get-EventSubscriber | Unregister-Event
            }
        }
        
        Write-Host "Download completed successfully!"
    }
    catch {
        Write-Error "Error: Download failed. Please check the URL and your connection."
        if (Test-Path $tempDownloadPath) { Remove-Item $tempDownloadPath }
        Read-Host "Press Enter to exit"
        exit 1
    }

    Write-Host "`nInstalling update..."
    if (Test-Path $localJarFile) {
        Write-Host " - Backing up old version to '$backupJarFile'..."
        Move-Item -Path $localJarFile -Destination $backupJarFile -Force
    }

    Write-Host " - Installing new version..."
    Move-Item -Path $tempDownloadPath -Destination $localJarFile -Force


    # --- Step 6: Update the local version file ---
    Write-Host " - Updating local version number..."
    # Create a new JSON object to ensure the file is clean
    @{ currentVersion = $remoteVersionInfo.latestVersion } | ConvertTo-Json -Depth 1 | Set-Content $localVersionFile


    # --- Step 7: Restart the application ---
    # !! CRITICAL !! You MUST customize this step.
    Write-Host "`nRestarting application (you may need to customize this step)..."
    # Example: Start-Process "java" -ArgumentList "-jar", $localJarFile
    # Example: Start-Process ".\start_client.bat"
    
    Write-Host "`nUpdate complete! Now running version $($remoteVersionInfo.latestVersion)."
}

# Pause at the end
Read-Host "`nPress Enter to exit."
