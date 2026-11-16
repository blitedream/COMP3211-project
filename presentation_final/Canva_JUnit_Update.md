# Canva 更新内容（待在线同步）

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

删除旧的“为什么不用 JUnit”回答。同步 Speaker_Scripts_Revised.txt 的正确数字发音：fifty-six / ninety-nine point three zero percent。检查新增小字无重叠，再导出并同步 presentation_final/PIM_Presentation.pdf 和 docs/final/05_Presentation.pdf。当前这两份 PDF 尚未更新，不能声称 Canva 已完成修改。
