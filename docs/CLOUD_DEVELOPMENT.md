# COMP3211 PIM cloud development handoff

This is the existing Java CLI course project, prepared for continued development
in a Linux development environment. Moving development to the cloud does not change the required
application into a website or introduce a GUI/backend.

## Start from the complete version

Repository: https://github.com/blitedream/COMP3211-project

Continue from branch `codex/cloud-dev-20261116`, which contains the integrated
application, tests and final documents. `main` contains an earlier member A
implementation. Preserve teammates' branches and changes.

```bash
git clone --branch codex/cloud-dev-20261116 https://github.com/blitedream/COMP3211-project.git
cd COMP3211-project
java -version
javac -version
bash pim.sh test
bash pim.sh coverage
bash pim.sh run
```

Use a JDK with `java` and `javac` on PATH, preferably JDK 21. The sources compile
to Java 8 bytecode. There are no application dependencies beyond the Java
standard library; the committed JARs in `lib/` are JUnit, Hamcrest and JaCoCo test tools only.
The Bash entry point uses Linux classpaths and supports Windows Git Bash too.
The existing PowerShell scripts and double-click Windows launcher remain usable.

## Baseline to verify in the new environment

- 56 named model cases: 9 core, 22 search, 19 persistence, 6 atomic-save.
- Three reporter checks and CLI integration checks pass separately.
- Locally measured model-only JaCoCo coverage: 428/431 lines (99.30%),
  257/268 branches (95.90%), 95/95 methods. See `docs/FINAL_ACCEPTANCE.txt`.
- The application and tests were verified on Windows with Temurin JDK 21.0.3
  and Azul Zulu JDK 8u512.
  Linux/cloud execution must be verified in the destination; it is not implied
  by that Windows result. Atomic replacement depends on filesystem support.
- `bash pim.sh coverage` writes fresh reports under `build/`, so verification
  does not overwrite the reviewed report in `coverage-report/`.

Do not add new product features just to perform the migration. First verify
the clone, tests and coverage; report any environment-specific failures before
claiming the cloud copy is ready. Future changes should stay within the user's
requested scope and preserve the CLI and Java standard-library constraints.

## Current artifacts and editing sources

- `docs/PIM_SRS.docx`: SRS v1.3 editing source. Edit this document directly.
- `tools/build_submission_docs.py` and `tools/document_details.py`: formal PDF
  generation and design detail sources (Python packages are needed only if
  regenerating documents; they are not Java application dependencies).
- `docs/final/`: formal SRS, design, manuals, presentation and recording guide.
- `Requirements_Coverage_Report.pdf` and `Model_Line_Coverage_Report.pdf`: submission reports.
- `docs/final/05_Presentation.pdf`: the single maintained eight-page Canva export.
  Save future Canva exports directly here; the PDF builder preserves this file.
- `presentation/Speaker_Scripts.txt`: current speaker script.
- `docs/FINAL_ACCEPTANCE.txt`: prior quality audit, including its limitations.
- `docs/SUBMISSION_STATUS.txt`: pending submission work.

## Work reserved for the team

The team must complete the two videos and official Honour Declaration.
After the team supplies `video/demo.mp4`, `video/presentation.mp4` and
`Honour_Declaration.pdf`, run `package_submission.ps1` on Windows and inspect the
final archive. Those files are intentionally ignored by Git.

The previous estimated score is not a teacher grade or a promise of full marks.
Migration readiness is separate from final submission readiness.
