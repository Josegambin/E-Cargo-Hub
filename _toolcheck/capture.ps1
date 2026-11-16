$adb = "C:\Users\Desktop\AppData\Local\Android\Sdk\platform-tools\adb.exe"
$s = "emulator-5554"
$td = "c:\Users\Desktop\Desktop\E-Cargo Hub\_toolcheck"
$out = Join-Path $env:TEMP "ecargo_cap.txt"
$r = @()

foreach ($d in @(0,2,3)) {
  $r += "=== capture display $d ==="
  $cap = & $adb -s $s shell screencap -p -d $d /sdcard/cap_$d.png 2>&1
  $r += "screencap: $cap"
  $pull = & $adb -s $s pull /sdcard/cap_$d.png (Join-Path $td "cap_$d.png") 2>&1
  $r += "pull: $pull"
}
$r -join "`n" | Out-File -Encoding utf8 $out
"DONE" | Out-File -Encoding utf8 $out -Append
