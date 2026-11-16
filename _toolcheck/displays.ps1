$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$s = "emulator-5554"
$td = "c:\Users\Desktop\Desktop\E-Cargo Hub\_toolcheck"
$out = Join-Path $env:TEMP "ecargo_i6.txt"
$r = @()

$r += "=== display ids ==="
$r += & $adb -s $s shell dumpsys SurfaceFlinger --display-id 2>&1 | Out-String

# capturar cada display en estado actual
$r += "=== displays list (dumpsys display) ==="
$r += & $adb -s $s shell dumpsys display 2>&1 | Select-String -Pattern "Display Id|uniqueId|mDisplayId|displayId=" | Select-Object -First 12 | Out-String

$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
