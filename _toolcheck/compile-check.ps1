$javahome = Join-Path $env:ProgramFiles "Android\Android Studio\jbr"
$proj = "c:\Users\Desktop\Desktop\E-Cargo Hub\backend-java\E-Cargo-Hub"
$out = Join-Path $env:TEMP "ecargo_compile3.txt"

$env:JAVA_HOME = $javahome
$env:Path = "$javahome\bin;$env:Path"
Set-Location $proj

$ErrorActionPreference = "Continue"
& ".\mvnw.cmd" -q -o compile *>&1 | Out-File -Encoding utf8 $out
"EXIT=$LASTEXITCODE" | Out-File -Encoding utf8 $out -Append
