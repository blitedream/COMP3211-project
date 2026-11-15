# COMP3211 Personal Information Management (PIM)

A Java command-line application for creating, updating, searching, printing,
deleting, saving and loading notes, tasks, events and contacts (US1-US11).
The application uses only Java standard libraries.

## Requirements

- Windows with PowerShell.
- A JDK with `java` and `javac` available on PATH.
- Verified with Eclipse Temurin JDK 21.0.3. Compilation targets Java 8;
  the complete application has not been separately verified on a Java 8 runtime.

No Maven, Gradle or runtime dependency installation is needed. The JAR files in
`lib/` are JaCoCo coverage tools, not application dependencies.

## Start the application

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
Creation prints a record ID; use that full ID for `print ID`, `delete ID`, or
`update ID text "Revised note"` (the latter applies to notes).

Quote arguments containing spaces. Dates use `yyyy-MM-dd HH:mm`.
Save overwrites the named file. A successful load replaces the current
collection; a failed load preserves it. Quit and end-of-input do not save
changes automatically. Relative file paths resolve from the launch directory.
See the [CLI user and developer guide](docs/D_CLI_manual.md) for all commands,
field names, error handling and development instructions.

## Run tests

Run the complete existing model and CLI integration suite:

```powershell
.\run_d_tests.ps1
```

To run only the A/B/C model suites:

```powershell
.\run_tests.ps1
```

Both scripts compile source and test files. A failed compilation or test returns
a nonzero exit code. The model suites currently report 6 core, 7 search and
9 persistence test groups; the additional CLI suite prints
`CLI integration checks passed` on success. These totals are test groups,
not counts of individual assertions or proof of complete coverage.

The existing [JaCoCo HTML report](coverage-report/index.html) is a stored report.
Running the commands above does **not** regenerate coverage measurements.

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
