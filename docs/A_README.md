# PIM Course Project

Current implementation: Member A's Java PIR core for US1-US6. Search, `.pim`
persistence, and the CLI are separate integration tasks.

## Requirements

Java 8 or newer is sufficient to compile and run the code. The test script
targets Java 8 and was verified with JDK 21 `javac` and a Java 8 runtime. The
model uses only Java standard library classes.

## Run A's tests on Windows

```powershell
.\run_tests.ps1
```

The public API is in `src/main/java/model/`. The design and integration
contract are in `docs/A_core_design.md`. The editable SRS draft is in
`docs/PIM_SRS_Draft.docx`.
