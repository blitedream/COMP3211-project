# COMP3211 Personal Information Management (PIM)

A Java command-line application for creating, updating, searching, printing,
deleting, saving and loading notes, tasks, events and contacts (US1-US11).
The application uses only Java standard libraries.

## Requirements

- Windows with PowerShell, or Linux / Linux development environment with Bash.
- A JDK with `java` and `javac` available on PATH.
- Verified with Eclipse Temurin JDK 21.0.3. Compilation targets Java 8;
  the current revision was tested on JDK 21.0.3, not re-tested on a Java 8 runtime.

No Maven, Gradle or runtime dependency installation is needed. The JAR files in
`lib/` are JUnit 4.13.2, Hamcrest 1.3 and JaCoCo test tools, not application dependencies.

## Start the application

On Windows, double-click **启动 PIM.cmd** in this folder. It opens the terminal,
compiles the source and launches the PIM prompt with quick-start examples.
Keep the launcher in the project folder. A JDK is still required; missing Java
or startup failures leave an explanation visible. No system execution-policy
setting is changed. Files saved using relative paths are placed in this project
folder. Use `save` before `quit`; there is no automatic save.


Open PowerShell in this repository's root directory and run:

```powershell
.\run.ps1
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
Creation prints a record ID. `print`, `delete` and `update` accept the full
36-character ID or any unique prefix of it (8 characters usually suffice).

Quote arguments containing spaces. Dates use `yyyy-MM-dd HH:mm`.
Save stages and flushes a same-directory temporary file before atomic replacement. Failed staging or unsupported replacement preserves the old destination; no unsafe fallback is used. A successful load replaces the current
collection; a failed load preserves it. Quit and end-of-input do not save
changes automatically. Relative file paths resolve from the launch directory.
See the [CLI user and developer guide](docs/D_CLI_manual.md) for all commands,
field names, error handling and development instructions.

## Run tests

Run the complete existing model and CLI integration suite:

```powershell
.\run_d_tests.ps1
```

To run only the model suites:

```powershell
.\run_tests.ps1
```

Both scripts compile source and test files. A failed compilation or test returns
a nonzero exit code. The model suites report 56 named test cases: 9 core,
22 search, 19 persistence and 6 atomic-save cases. The additional CLI suite
prints `CLI integration checks passed` on success. Test counts alone do not
prove complete coverage.

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

Cloud coverage output is written to `build/cloud-coverage-report/index.html`
and `build/cloud-coverage.xml`, leaving the submitted report intact until the
new measurements have been reviewed. These commands work from any directory;
relative PIM save paths resolve inside this repository.
See [cloud development handoff](docs/CLOUD_DEVELOPMENT.md) for the verified
baseline, document sources and remaining human deliverables.

## Project structure and contributions

| Location | Purpose |
| --- | --- |
| `src/main/java/model/` | A: record model; B: search; C: record operations and persistence |
| `src/main/java/cli/` | D: command-line interface and integration |
| `src/test/java/model/` | Automated model tests |
| `src/test/java/cli/` | CLI integration checks |
| `run.ps1` | Compile and start the application |
| `run_tests.ps1` | Compile and run model tests |
| `run_d_tests.ps1` | Run model tests followed by CLI checks |
| `docs/` | Requirements draft, module designs and CLI guide |

## Documentation

- [SRS draft](docs/PIM_SRS_Draft.docx)
- [A module guide](docs/A_README.md) and [core design](docs/A_core_design.md)
- [B search design and expression grammar](docs/B_search_design.md)
- [C persistence and record operations](docs/C_persistence_design.md)
- [D CLI user/developer guide and integration design](docs/D_CLI_manual.md)

The module documents are working material. This README does not replace the
course's required formal manuals, consolidated design document, requirements
coverage report, coverage report, videos or presentation deliverables.

## Build the submission archive

1. Fill `Honour_Declaration_TEMPLATE.docx`, export it as PDF and save it as
   `Honour_Declaration.pdf` in the repository root.
2. Record `video/demo.mp4` (at most 4 minutes) and `video/presentation.mp4`
   (at most 5 minutes).
3. Run:

```powershell
.\make_submission.ps1
```

The script writes `build/COMP3211_submission_<timestamp>.zip` and verifies that
`Honour_Declaration.pdf` sits at the archive root, as the course requires.


## JUnit model tests

The 56 model cases use JUnit 4 `@Test` and `JUnitCore`. Run `bash pim.sh test` or `./run_tests.ps1`; CLI integration checks remain a separate executable. Add both JUnit and Hamcrest JARs from `lib/` to the IDE test classpath. Application execution needs neither JAR.
