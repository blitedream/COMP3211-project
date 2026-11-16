$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$mainOut = Join-Path $root 'build\classes'
$testOut = Join-Path $root 'build\test-classes'
$testLibs = "$root/lib/junit-4.13.2.jar;$root/lib/hamcrest-core-1.3.jar"
New-Item -ItemType Directory -Force -Path $mainOut, $testOut | Out-Null
$mainSources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\main\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
$testSources = @(Get-ChildItem -LiteralPath (Join-Path $root 'src\test\java') -Filter '*.java' -Recurse | ForEach-Object FullName)
& javac -encoding UTF-8 -g -Xlint:-options -source 8 -target 8 -d $mainOut @mainSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& javac -encoding UTF-8 -g -Xlint:-options -source 8 -target 8 -cp "$mainOut;$testLibs" -d $testOut @testSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$mainOut;$testOut;$testLibs" org.junit.runner.JUnitCore model.ModelCoreTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$mainOut;$testOut;$testLibs" org.junit.runner.JUnitCore model.SearchTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$mainOut;$testOut;$testLibs" org.junit.runner.JUnitCore model.PersistenceTest
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$mainOut;$testOut;$testLibs" org.junit.runner.JUnitCore model.AtomicSaveTest
exit $LASTEXITCODE
