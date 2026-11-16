$ErrorActionPreference = 'Stop'
& "$PSScriptRoot/test_model.ps1"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
& java -cp "$PSScriptRoot/build/classes;$PSScriptRoot/build/test-classes" cli.CliTest
exit $LASTEXITCODE
