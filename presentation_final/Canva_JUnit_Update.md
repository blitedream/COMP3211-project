# Canva 更新内容（已在线同步，2026-11-17）

已在原 Canva 设计中完成第 8 页的文字、演讲备注和 JUnit 问答更新，并导出完整 8 页 PDF。已检查新增页脚无重叠、无溢出；导出 PDF 的前 7 页文字与上一版一致。

保留原有 8 页结构和视觉设计。第 1–7 页产品功能和架构不变；JUnit 是测试依赖，不放进应用架构图。

## 第 8 页：LESSON / UNIT TESTING

保留标题：Test the saved state after a failure
保留 Given / When / Then 场景与原文件字节相等、临时文件清理断言。

将 56 下方的标签从 `named model cases` 改为 `JUnit model tests`。
增加一行小字：`JUnit 4.13.2 · JaCoCo 0.8.11 · Model-only coverage`
保留数据：56 / 99.30% model line coverage / 95.90% model branch coverage。
页脚改为：`Verified on Linux / JDK 21.0.12.1 · 16 Nov 2026. Coverage is execution evidence, not proof of correctness.`

## 第 8 页演讲备注

Our lesson is to test the state after failure, not only the error message. Suppose a saved collection already exists and a staged write fails midway. The test compares the original file byte for byte and checks that the temporary file is removed. A thrown exception alone would miss data loss. This led to staging before atomic replacement. All 56 JUnit model tests pass, with 99.30 percent line coverage measured by JaCoCo. Coverage shows execution; the state assertions provide evidence about behaviour. Thank you.

## 问答备注

Why JUnit? JUnit 4.13.2 runs the model tests through @Test and JUnitCore, reports failures and returns a nonzero exit code on failure. JUnit and Hamcrest are test dependencies only. The application still uses only Java standard libraries. CLI integration checks run separately.

## 导出检查

第 8 页备注已采用正确的 JUnit 问答，并包含 fifty-six / ninety-nine point three zero percent 的读法；与 Speaker_Scripts_Revised.txt 一致。完整导出已同步到 presentation_final/PIM_Presentation.pdf 和 docs/final/05_Presentation.pdf，两份文件的 SHA-256 相同。

本地部署基线为 87c6b32。Windows / Temurin JDK 21.0.3 复验：56 项 JUnit 模型测试及独立 CLI 集成检查通过；行覆盖 428/431，分支覆盖 257/268，方法覆盖 95/95。创建、保存、载入、打印及退出冒烟检查通过。测试日志与新覆盖率报告保存在忽略的 build/ 目录。幻灯片保留原 Linux / JDK 21.0.12.1 的 16 Nov 2026 验证信息。
