## Context

首页周次切换当前只有 prev/next 按钮 + 数字 EditText，用户需要知道总周数范围才能正确输入。设置页包含学期开始日期（`btnSemesterStart`）和总周数（`etTotalWeeks`），这些是学期级配置，与 `SemesterManageActivity` 的编辑对话框（`dialog_semester_edit.xml` 已有 startDate + totalWeeks）重复。

设置页当前结构：学期区（日期按钮 + 管理学期 + 管理课表 + 导入帮助 + 总周数）→ 节次区 → 外观区 → 导出。移除日期和总周数后，学期区只剩下入口按钮，可以更简洁。

## Goals / Non-Goals

**Goals:**
- 首页点击周次输入框弹出周次列表对话框（1..totalWeeks，标注"本周"）
- 设置页移除 `btnSemesterStart` 和 `etTotalWeeks`
- 设置页排版精简

**Non-Goals:**
- 不修改 `SemesterManageActivity` 的编辑对话框（已有日期和总周数编辑）
- 不修改节次时间编辑器

## Decisions

### D1: 周次选择对话框用 AlertDialog + setItems

点击 `etWeek` 时弹出 `AlertDialog.setItems(weekLabels, ...)`，`weekLabels` 格式如 `["第1周 (本周)", "第2周", ...]`，总项数从 `activeSemester.totalWeeks` 获取。

**替代方案**: 用 NumberPicker。放弃——NumberPicker 占据空间大，列表更直观。

### D2: 保留输入框手动输入功能

EditText 仍可手动输入数字（保留 IME_ACTION_DONE 监听），对话框是补充而非替代。点击弹出对话框，但仍有手动输入能力。

### D3: 设置页移除日期/总周数后的布局

移除 `btnSemesterStart`、`etTotalWeeks` 及其包裹的 LinearLayout。学期区只保留：管理学期、管理课表、导入帮助三个按钮。分割线保留在学期区与节次区之间。

### D4: SettingsActivity 移除日期/总周数相关代码

移除 `pickSemesterStart()`、`loadConfig()` 中的日期/总周数设置、`saveConfig()` 中的总周数保存、`onPause()` 中的 saveConfig 调用。保留 `configRepository` 用于 `getCachedOrDefault()`（导入帮助等可能用到）。

## Risks / Trade-offs

- [移除设置页日期编辑后用户需进入学期管理修改] → 学期管理编辑对话框已有该功能，职责更清晰
- [周次对话框项数可能很多（如 20 周）] → AlertDialog 列表原生支持滚动，无性能问题
