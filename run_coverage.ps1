$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/run_d_tests.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
$execFile = Join-Path $PSScriptRoot 'build/model-coverage.exec'
$agent = Join-Path $PSScriptRoot 'lib/org.jacoco.agent-0.8.11-runtime.jar'
$cp = "$PSScriptRoot/build/classes;$PSScriptRoot/build/test-classes"
$append = 'false'
foreach ($suite in @('model.ModelCoreTest','model.SearchTest','model.PersistenceTest')) {
 & java "-javaagent:$agent=destfile=$execFile,append=$append" -cp $cp $suite
 if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
 $append = 'true'
}
& java -jar "$PSScriptRoot/lib/org.jacoco.cli-0.8.11-nodeps.jar" report $execFile --classfiles "$PSScriptRoot/build/classes/model" --sourcefiles "$PSScriptRoot/src/main/java" --html "$PSScriptRoot/coverage-report" --xml "$PSScriptRoot/build/coverage.xml"
exit $LASTEXITCODE
