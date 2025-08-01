# PowerShell Build Script for Android Calendar App
# Sets up ADB path and builds/installs the app

# Set ADB path from Android Studio installation
$ADB_PATH = $null

# Try multiple possible ADB paths
$possiblePaths = @(
    "C:\AStudio\Sdk\platform-tools\adb.exe",
    "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    "C:\Android\Sdk\platform-tools\adb.exe",
    "C:\Program Files\Android\Android Studio\Sdk\platform-tools\adb.exe"
)

# Find the first existing ADB path
foreach ($path in $possiblePaths) {
    if (Test-Path $path) {
        $ADB_PATH = $path
        Write-Host "ADB found at: $ADB_PATH" -ForegroundColor Green
        break
    }
}

# If no ADB found, try to find it in PATH
if (-not $ADB_PATH) {
    try {
        $adbInPath = Get-Command adb -ErrorAction SilentlyContinue
        if ($adbInPath) {
            $ADB_PATH = $adbInPath.Source
            Write-Host "ADB found in PATH at: $ADB_PATH" -ForegroundColor Green
        }
    } catch {
        # ADB not in PATH
    }
}

# If still no ADB found, show error and exit
if (-not $ADB_PATH) {
    Write-Host "ADB not found in any of the expected locations:" -ForegroundColor Red
    foreach ($path in $possiblePaths) {
        Write-Host "  - $path" -ForegroundColor Gray
    }
    Write-Host "Please install Android Studio or set up ADB manually" -ForegroundColor Yellow
    Write-Host "You can also run .\set-adb-path.ps1 to configure ADB path" -ForegroundColor Yellow
    exit 1
}

# Function to check if device is connected
function Test-DeviceConnected {
    try {
        $devices = & $ADB_PATH devices
        $connected = $devices | Select-String "device$"
        if ($connected) {
            Write-Host "Device connected: $($connected.Line)" -ForegroundColor Green
            return $true
        } else {
            Write-Host "No device connected. Please connect your device and enable USB debugging." -ForegroundColor Red
            Write-Host "Make sure:" -ForegroundColor Yellow
            Write-Host "  - USB debugging is enabled in Developer Options" -ForegroundColor Gray
            Write-Host "  - Device is connected via USB" -ForegroundColor Gray
            Write-Host "  - You've authorized the computer on your device" -ForegroundColor Gray
            return $false
        }
    } catch {
        Write-Host "Error checking device connection: $_" -ForegroundColor Red
        return $false
    }
}

# Function to build the app
function Build-App {
    Write-Host "Building Android app..." -ForegroundColor Cyan
    try {
        .\gradlew.bat assembleRelease
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Build successful!" -ForegroundColor Green
            return $true
        } else {
            Write-Host "Build failed!" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "Build error: $_" -ForegroundColor Red
        return $false
    }
}

# Function to install the app
function Install-App {
    $apkPath = "app\build\outputs\apk\release\app-release.apk"
    
    # Check if APK exists
    if (-not (Test-Path $apkPath)) {
        Write-Host "APK file not found at: $apkPath" -ForegroundColor Red
        Write-Host "Please build the app first or check the build output directory" -ForegroundColor Yellow
        return $false
    }
    
    Write-Host "Installing app on device..." -ForegroundColor Cyan
    try {
        $installCommand = "$ADB_PATH install `"$apkPath`""
        Write-Host "Running: $installCommand" -ForegroundColor Gray
        Invoke-Expression $installCommand
        if ($LASTEXITCODE -eq 0) {
            Write-Host "App installed successfully!" -ForegroundColor Green
            return $true
        } else {
            Write-Host "Installation failed!" -ForegroundColor Red
            return $false
        }
    } catch {
        Write-Host "Installation error: $_" -ForegroundColor Red
        return $false
    }
}

# Function to start the app
function Start-App {
    Write-Host "Starting app..." -ForegroundColor Cyan
    try {
        $startCommand = "$ADB_PATH shell am start -a android.intent.action.MAIN -c android.intent.category.LAUNCHER -n com.calendar.tbt/.MainActivity"
        Write-Host "Running: $startCommand" -ForegroundColor Gray
        Invoke-Expression $startCommand
        if ($LASTEXITCODE -eq 0) {
            Write-Host "App started successfully!" -ForegroundColor Green
        } else {
            Write-Host "Failed to start app!" -ForegroundColor Red
        }
    } catch {
        Write-Host "Start app error: $_" -ForegroundColor Red
    }
}

# Main execution
Write-Host "=== Android Calendar App Build Script ===" -ForegroundColor Magenta
Write-Host ""

# Check if device is connected
if (Test-DeviceConnected) {
    # Build the app
    if (Build-App) {
        # Install the app
        if (Install-App) {
            # Start the app
            Start-App
        }
    }
} else {
    Write-Host "Please connect a device and try again." -ForegroundColor Yellow
}

Write-Host ""
Write-Host "=== Build Script Complete ===" -ForegroundColor Magenta 