#!/usr/bin/env bash
# Linux / Codex Cloud entry point; also supports Git Bash on Windows.
set -euo pipefail
cd -- "$(dirname -- "${BASH_SOURCE[0]}")"

mode="${1:-run}"
case "$mode" in
  run|test|coverage) ;;
  *) printf 'Usage: bash pim.sh [run|test|coverage]\n' >&2; exit 2 ;;
esac
for program in java javac; do
  command -v "$program" >/dev/null || { printf 'A JDK with %s on PATH is required.\n' "$program" >&2; exit 1; }
done

separator=:
case "$(uname -s)" in MINGW*|MSYS*|CYGWIN*) separator=';' ;; esac
classpath="build/classes${separator}build/test-classes"
main_sources=()
while IFS= read -r -d '' source; do main_sources+=("$source"); done < <(find src/main/java -name '*.java' -print0)
mkdir -p build/classes
javac -encoding UTF-8 -g -Xlint:-options -source 8 -target 8 -d build/classes "${main_sources[@]}"

if [[ "$mode" == run ]]; then
  exec java -Dfile.encoding=UTF-8 -cp build/classes cli.PIMCli
fi

test_sources=()
while IFS= read -r -d '' source; do test_sources+=("$source"); done < <(find src/test/java -name '*.java' -print0)
mkdir -p build/test-classes
javac -encoding UTF-8 -g -Xlint:-options -source 8 -target 8 -cp build/classes -d build/test-classes "${test_sources[@]}"
suites=(model.ModelCoreTest model.SearchTest model.PersistenceTest model.AtomicSaveTest)
for suite in "${suites[@]}"; do
  java -Dfile.encoding=UTF-8 -cp "$classpath" "$suite"
done
java -Dfile.encoding=UTF-8 -cp "$classpath" cli.CliTest

if [[ "$mode" == coverage ]]; then
  append=false
  for suite in "${suites[@]}"; do
    java "-javaagent:lib/org.jacoco.agent-0.8.11-runtime.jar=destfile=build/model-coverage.exec,append=$append" -Dfile.encoding=UTF-8 -cp "$classpath" "$suite"
    append=true
  done
  java -jar lib/org.jacoco.cli-0.8.11-nodeps.jar report build/model-coverage.exec \
    --classfiles build/classes/model --sourcefiles src/main/java \
    --html build/cloud-coverage-report --xml build/cloud-coverage.xml 2>&1 | tee build/cloud-coverage-report.log
  if grep -q '\[WARN\]' build/cloud-coverage-report.log; then
    printf 'Coverage report has warnings; do not publish it.\n' >&2
    exit 1
  fi
fi
