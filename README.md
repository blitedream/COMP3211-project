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
Creation prints a record ID. `print`, `delete` and `update` accept the full
36-character ID or any unique prefix of it (8 characters usually suffice).

Quote arguments containing spaces. Dates use `yyyy-MM-dd HH:mm`.
Save stages and flushes a same-directory temporary file before atomic replacement. Failed staging or unsupported replacement preserves the old destination; no unsafe fallback is used. A successful load replaces the current
collection; a failed load preserves it. Quit and end-of-input do not save
changes automatically. Relative file paths resolve from the launch directory.
See the [user manual](docs/final/03_User_Manual.pdf) for all commands, field names
and error handling, and the [developer manual](docs/final/04_Developer_Manual.pdf)
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

## Project structure and contributions

| Location | Purpose |
| --- | --- |
| `src/main/java/model/` | A: record model; B: search; C: record operations and persistence |
| `src/main/java/cli/` | D: command-line interface and integration |
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
| `docs/final/` | Formal SRS, design, manuals, slides and recording guide |

## Documentation

- [Software requirements specification](docs/final/01_SRS.pdf)
- [Design document and API reference](docs/final/02_Design.pdf)
- [User manual](docs/final/03_User_Manual.pdf)
- [Developer manual](docs/final/04_Developer_Manual.pdf)
- [Presentation slides](docs/final/05_Presentation.pdf)
- [Requirements coverage](Requirements_Coverage_Report.pdf)
- [Model line coverage](Model_Line_Coverage_Report.pdf)

The ZIP stores the SRS, design, manuals and slides under `documents/`.
Both coverage reports are at the ZIP root.

## JUnit model tests

The 56 model cases use JUnit 4 `@Test` and `JUnitCore` through
`testsupport.NamedTestRunner`, which prints each case's name and outcome.
Run `bash pim.sh test` or `./test_model.ps1`; CLI integration checks remain a
separate executable. Add both JUnit and Hamcrest JARs from `lib/` to the IDE
test classpath. Application execution needs neither JAR.

<!-- REPOSITORY-ONLY: the packager omits the following authoring instructions. -->
## Build the submission archive

1. Fill `Honour_Declaration_Template.docx`, export it as PDF and save it as
   `Honour_Declaration.pdf` in the repository root.
2. Record `video/demo.mp4` (at most 4 minutes) and `video/presentation.mp4`
   (at most 5 minutes).
3. Run:

```powershell
.\package_submission.ps1
```

The script writes `build/COMP3211_submission_<timestamp>.zip` and verifies that
`Honour_Declaration.pdf` sits at the archive root, as the course requires.

Before the human deliverables are ready, use `./package_submission.ps1 -Preview`
to create `build/COMP3211_PREVIEW_NOT_FOR_SUBMISSION_<timestamp>.zip` for an
unpack-and-run check. It includes a prominent `NOT_FOR_SUBMISSION.txt` listing
missing materials. The normal command still refuses to produce a submission
without both MP4 files and the completed Honour Declaration. File presence
does not verify signatures, contribution percentages, video timing or faces;
the group must review these before submission.

The [recording guide](docs/final/06_Recording_Guide.pdf) and rehearsal script in
`presentation/` are for group preparation and are not included in the ZIP.
The single maintained presentation PDF is `docs/final/05_Presentation.pdf`.
Save new Canva exports there. Current document builders are in `tools/`;
old presentation drafts and obsolete generators have been removed.
