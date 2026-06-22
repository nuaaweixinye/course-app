## 1. 周次选择对话框

- [x] 1.1 `MainActivity`：为 `etWeek` 添加 `OnClickListener`，点击时弹出 `AlertDialog.setItems()` 周次列表
- [x] 1.2 周次列表数据源从 `viewModel.getActiveSemester().getValue().totalWeeks` 获取，标注当前周为"(本周)"
- [x] 1.3 点击列表项调用 `viewModel.setSelectedWeek(weekNo)` 并关闭对话框
- [x] 1.4 保留手动输入功能（`OnEditorActionListener` 不变），添加 `setFocusable(false)` 或 `setOnClickListener` 覆盖默认点击行为避免冲突

## 2. 设置页精简

- [x] 2.1 `activity_settings.xml`：移除 `btnSemesterStart` 按钮
- [x] 2.2 `activity_settings.xml`：移除总周数 LinearLayout（`label_total_weeks` + `etTotalWeeks`）
- [x] 2.3 `activity_settings.xml`：排版优化（学期区只留管理学期/管理课表/导入帮助；确认分割线间距合理）
- [x] 2.4 `SettingsActivity.java`：移除 `pickSemesterStart()` 方法
- [x] 2.5 `SettingsActivity.java`：移除 `loadConfig()` 中日期/总周数设置代码
- [x] 2.6 `SettingsActivity.java`：移除 `saveConfig()` 中总周数保存逻辑、`onPause()` 中的 `saveConfig()` 调用
- [x] 2.7 `SettingsActivity.java`：移除 `parseTotalWeeks()` 方法

## 3. 字符串资源

- [x] 3.1 `strings.xml`：新增 `dialog_select_week` = "选择周次"，`label_this_week_suffix` = "(本周)"

## 4. 构建与验证

- [x] 4.1 编译通过
- [x] 4.2 安装并测试：点击周次弹出对话框、选周切换、设置页无日期/总周数、学期编辑对话框仍有日期/总周数
