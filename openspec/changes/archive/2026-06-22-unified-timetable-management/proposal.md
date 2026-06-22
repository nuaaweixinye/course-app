## Why

学期和课表的管理入口纠缠不清。当前 `TimetableManageActivity` 只能从 `SemesterManageActivity` 的行内按钮进入，只显示单个学期的课表；用户无法在一个地方看到所有课表的全貌。同时学期管理页面混杂了课表管理入口，职责不清。新建课表时无法选择关联学期，只能绑定进入时的那个学期。

## What Changes

- **BREAKING**: `TimetableManageActivity` 改为显示**所有学期**的全部课表，每行标注所属学期名称
- **BREAKING**: `SemesterManageActivity` 移除"课表管理"行内按钮，学期页面只管理学期的增删改
- `TimetableManageActivity` 新建课表时弹出学期选择器（Spinner），用户选择关联学期
- `SettingsActivity` 的"管理课表"按钮直接打开 `TimetableManageActivity`（不再需要先选学期）
- 导入课表时同样可选择目标学期（或默认使用当前活跃学期）

## Non-goals

- 不修改数据库 schema（TimetableEntity 已有 semesterId 字段）
- 不修改课表切换的联动逻辑（`switchTo` 的学期联动已实现）
- 不修改课程编辑页的课表选择逻辑
- 不新增独立的课表管理底部导航入口

## Capabilities

### New Capabilities
_(无)_

### Modified Capabilities
- `timetable-entity`: 课表列表从按学期过滤改为全局展示；新建课表时可选择任意学期
- `app-settings`: 设置页"管理课表"入口直接打开全局课表列表；学期管理移除课表入口

## Impact

- `TimetableManageActivity`: 不再接收 `EXTRA_SEMESTER_ID`，改为加载所有课表 + 学期名称映射；新建对话框增加学期 Spinner
- `SemesterManageActivity`: 移除 `openTimetableManage()` 方法和"课表管理"按钮
- `SettingsActivity`: "管理课表"按钮启动 `TimetableManageActivity`（无 Extra）
- `TimetableDao`: 新增 `observeAllWithSemester()` 查询，JOIN semesters 获取学期名
- `item_semester_row.xml`: 移除"课表管理"按钮
- `item_timetable_row.xml`: 增加学期名称显示
