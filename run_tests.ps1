$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mainOut = Join-Path $root 'build\classes'
$testOut = Join-Path $root 'build\test-classes'
New-Item -ItemType Directory -Force -Path $mainOut, $testOut | Out-Null
$mainSources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
& javac -Xlint:-options -source 8 -target 8 -d $mainOut @mainSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& javac -Xlint:-options -source 8 -target 8 -cp $mainOut -d $testOut @testSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$mainOut;$testOut" model.ModelCoreTest
exit $LASTEXITCODE
