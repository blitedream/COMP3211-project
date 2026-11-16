$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/run_d_tests.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
# Agent option strings can corrupt non-ASCII absolute paths on Windows.
# Keep all agent/report paths relative to the project directory.
Push-Location -LiteralPath $PSScriptRoot
try {
    & java '-javaagent:lib/org.jacoco.agent-0.8.11-runtime.jar=destfile=build/model-coverage.exec,append=false' -cp 'build/classes;build/test-classes;lib/junit-4.13.2.jar;lib/hamcrest-core-1.3.jar' testsupport.NamedTestRunner model.ModelCoreTest model.SearchTest model.PersistenceTest model.AtomicSaveTest
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    $report = & java -jar lib/org.jacoco.cli-0.8.11-nodeps.jar report build/model-coverage.exec --classfiles build/classes/model --sourcefiles src/main/java --html coverage-report --xml build/coverage.xml 2>&1
    $code = $LASTEXITCODE
    $report | Write-Output
    if ($code -ne 0 -or ($report -match '\[WARN\]')) { throw 'Coverage report failed validation; do not publish it.' }
} finally { Pop-Location }
