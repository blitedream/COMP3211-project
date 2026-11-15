$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$outDir = Join-Path $root 'build/classes'
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$sources = @(Get-ChildItem (Join-Path $root 'src/main/java') -Recurse -Filter '*.java' | ForEach-Object FullName)
& javac -encoding UTF-8 -Xlint:-options -source 8 -target 8 -d $outDir @sources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java '-Dfile.encoding=UTF-8' -cp $outDir cli.PIMCli
exit $LASTEXITCODE

