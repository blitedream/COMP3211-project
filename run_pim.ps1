$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$outDir = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
# The CLI reads and writes UTF-8; switch the console to UTF-8 so typed non-ASCII text is not garbled.
try { chcp 65001 | Out-Null } catch { }
$sources = @(Get-ChildItem (Join-Path $root 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 -Xlint:-options -source 8 -target 8 -d $outDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Dfile.encoding=UTF-8' -cp $outDir cli.PIMCli
exit $LASTEXITCODE

