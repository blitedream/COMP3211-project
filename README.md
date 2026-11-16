# COMP3211 Personal Information Management (PIM)

A Java command-line application for creating, updating, searching, printing,
deleting, saving and loading notes, tasks, events and contacts (US1-US11).
The application uses only Java standard libraries.

## Requirements

- Windows with PowerShell, or Linux / Linux development environment with Bash.
- A JDK with `java` and `javac` available on PATH.
- Verified on Windows with Eclipse Temurin JDK 21.0.3 and Azul Zulu JDK
  8u512 (1.8.0_512-b09). Both compiled and passed the model, reporter and CLI
  checks on 17 November 2026. Compilation targets Java 8.

No Maven, Gradle or runtime dependency installation is needed. The JAR files in
`lib/` are JUnit 4.13.2, Hamcrest 1.3 and JaCoCo test tools, not application dependencies.

## Start the application

On Windows, double-click **Start_PIM.cmd** in this folder. It opens the terminal,
compiles the source and launches the PIM prompt with quick-start examples.
The terminal uses a dark background, colored results and record panels when ANSI
color is supported. `list` shows easy record numbers; no UUID copying is needed.
Keep the launcher in the project folder. A JDK is still required; missing Java
or startup failures leave an explanation visible. No system execution-policy
setting is changed. Files saved using relative paths are placed in this project
folder. Use `save` before `quit`; there is no automatic save.


Open PowerShell in this repository's root directory and run:

```powershell
.\run_pim.ps1
```

The script compiles the application into `build/classes` and starts
`cli.PIMCli`. Enter the following commands at the application's `pim>` prompt,
not in PowerShell:

```text
help
create note "Finish COMP3211 project"
create task "Prepare demo" "2026-11-19 18:00"
print all
search type = task
save "my-records.pim"
quit
```

To restore those records in a later session, enter `load "my-records.pim"`.
Creation assigns a short record number. Type `list`, then use the displayed
number in commands such as `print 2` or `update 2 deadline "2026-11-20 18:00"`.
`#2` explicitly refers to that number. Search and deletion never renumber records;
new records never reuse old numbers during the same session. Loading preserves
numbers for known IDs and assigns fresh numbers to newly encountered IDs.
Numbers are session-only: after restarting, run `list` again. The stable IDs stored
in existing `.pim` files are unchanged. Full IDs and unique non-numeric prefixes
still work; `id:VALUE` explicitly selects a stored ID or any unique prefix.
If a numeric stored ID conflicts with a displayed number, PIM rejects the
ambiguous command and explains how to choose `#NUMBER` or `id:VALUE`.

Use `clear` to redraw the welcome panel. `help` includes update examples using
your current records. The prompt indicates unsaved changes. For plain text use
`powershell -File .\run_pim.ps1 -Plain`, or set `NO_COLOR`; Java's entry point also
accepts `--plain` and `--color`. Redirected output uses plain text automatically.

Quote arguments containing spaces. Dates use `yyyy-MM-dd HH:mm`.
Save stages and flushes a same-directory temporary file before atomic replacement. Failed staging or unsupported replacement preserves the old destination; no unsafe fallback is used. A successful load replaces the current
collection; a failed load preserves it. Quit and end-of-input do not save
changes automatically. Relative file paths resolve from the launch directory.
See the [user manual](documents/03_User_Manual.pdf) for all commands, field names
and error handling, and the [developer manual](documents/04_Developer_Manual.pdf)
for build and debugging instructions.

## Run tests

Run the complete existing model and CLI integration suite:

```powershell
.\test_all.ps1
```

To run only the model suites:

```powershell
.\test_model.ps1
```

Both scripts compile source and test files. A failed compilation or test returns
a nonzero exit code. The model suites report a named `[PASS]` or `[FAIL]` for
each of 56 cases: 9 core, 22 search, 19 persistence and 6 atomic-save cases.
JUnit continues running the remaining cases after a failure. Three separate
test-infrastructure checks verify the reporter; they do not count toward the
56 model cases or model coverage. The additional CLI suite
prints `CLI integration checks passed` on success.

The existing [JaCoCo HTML report](coverage-report/index.html) is a stored report.
Running the commands above does **not** regenerate coverage measurements.

## Linux / Linux development environment

Use a JDK (21 recommended to match the locally verified version), Bash and Git.
No Maven, Gradle or application dependency installation is required.

```bash
bash pim.sh test       # 56 model cases and CLI integration checks
bash pim.sh run        # interactive pim> prompt
bash pim.sh coverage   # tests and fresh model-only JaCoCo measurements
```

Coverage output is written to `build/cloud-coverage-report/index.html`
and `build/cloud-coverage.xml`. These commands work from any directory;
relative PIM save paths resolve inside this repository.

## Project structure

| Location | Purpose |
| --- | --- |
| `src/main/java/model/` | Record model, operations, search and persistence |
| `src/main/java/cli/` | Command-line interface and integration |
| `src/test/java/model/` | Automated model tests |
| `src/test/java/cli/` | CLI integration checks |
| `Start_PIM.cmd` | Double-click launcher for Windows |
| `run_pim.ps1` | Compile and start the application |
| `test_model.ps1` | Compile and run model tests |
| `test_all.ps1` | Run model tests followed by CLI checks |
| `generate_coverage.ps1` | Run all tests and regenerate model coverage |
| `pim.sh` | Bash entry point for running, testing and coverage |
| `lib/` | Bundled test and coverage tools |
| `coverage-report/` | Detailed JaCoCo HTML report |
| `documents/` | Final SRS, design, user/developer manuals and presentation slides |

## Documentation

- [Software requirements specification](documents/01_SRS.pdf)
- [Design document and API reference](documents/02_Design.pdf)
- [User manual](documents/03_User_Manual.pdf)
- [Developer manual](documents/04_Developer_Manual.pdf)
- [Presentation slides](documents/05_Presentation.pdf)
- [Requirements coverage](Requirements_Coverage_Report.pdf)
- [Model line coverage](Model_Line_Coverage_Report.pdf)

The five course documents are under `documents/`. Both coverage reports are at
the project root. The completed submission must also contain
`Honour_Declaration.pdf` at the root and `video/demo.mp4` and
`video/presentation.mp4`. Exclude `.git`, IDE settings and generated `build/`
files when creating the submission ZIP.

## JUnit model tests

The 56 model cases use JUnit 4 `@Test` and `JUnitCore` through
`testsupport.NamedTestRunner`, which prints each case's name and outcome.
Run `bash pim.sh test` or `./test_model.ps1`; CLI integration checks remain a
separate executable. Add both JUnit and Hamcrest JARs from `lib/` to the IDE
test classpath. Application execution needs neither JAR.
