# PowerShell script to set ADB path in environment variables
# Run this script to add Android SDK platform-tools to your PATH

$ADB_PATH = "C:\AStudio\Sdk\platform-tools"

# Check if the path exists
if (Test-Path $ADB_PATH) {
    Write-Host "✅ Android SDK platform-tools found at: $ADB_PATH" -ForegroundColor Green
    
    # Add to current session PATH
    $env:PATH += ";$ADB_PATH"
    Write-Host "✅ Added to current session PATH" -ForegroundColor Green
    
    # Add to user PATH permanently
    $userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
    if ($userPath -notlike "*$ADB_PATH*") {
        [Environment]::SetEnvironmentVariable("PATH", "$userPath;$ADB_PATH", "User")
        Write-Host "✅ Added to permanent user PATH" -ForegroundColor Green
        Write-Host "Note: You may need to restart your terminal for changes to take effect" -ForegroundColor Yellow
    } else {
        Write-Host "ℹ️  Path already exists in user PATH" -ForegroundColor Blue
    }
    
    # Test ADB
    try {
        $adbVersion = & "$ADB_PATH\adb.exe" version
        Write-Host "✅ ADB test successful:" -ForegroundColor Green
        Write-Host $adbVersion[0] -ForegroundColor Cyan
    } catch {
        Write-Host "❌ ADB test failed: $_" -ForegroundColor Red
    }
    
} else {
    Write-Host "❌ Android SDK platform-tools not found at: $ADB_PATH" -ForegroundColor Red
    Write-Host "Please check your Android Studio installation path" -ForegroundColor Yellow
    Write-Host "Common paths:" -ForegroundColor Yellow
    Write-Host "  - C:\Users\$env:USERNAME\AppData\Local\Android\Sdk\platform-tools" -ForegroundColor Gray
    Write-Host "  - C:\Android\Sdk\platform-tools" -ForegroundColor Gray
    Write-Host "  - C:\AStudio\Sdk\platform-tools" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== ADB Path Setup Complete ===" -ForegroundColor Magenta 