## Context

当前 `TimetableManageActivity` 通过 `EXTRA_SEMESTER_ID` 从 `SemesterManageActivity` 的行内按钮启动，只展示单个学期的课表。用户无法看到全局课表列表，也不清楚哪个课表属于哪个学期。`SemesterManageActivity` 的行内"课表管理"按钮混合了两个不同实体的管理职责。

技术现状：
- `TimetableDao` 有 `observeBySemester(semesterId)` 但没有 `observeAll()` 的 JOIN 查询
- `TimetableRepository` 有 `observeBySemester()` 和 `listBySemester()`
- `TimetableManageActivity` 依赖 `semesterId` Extra，新建/导入都绑定到该 semesterId
- `item_semester_row.xml` 有 `btnManageTimetables` 按钮
- `item_timetable_row.xml` 只有一个 `tvTimetableName` TextView

## Goals / Non-Goals

**Goals:**
- `TimetableManageActivity` 展示所有课表，每行标注所属学期
- 新建课表时用户可选择关联学期
- `SemesterManageActivity` 只管理学期的增删改
- 设置页直接打开全局课表管理

**Non-Goals:**
- 不修改课表切换的联动逻辑
- 不修改课程编辑页的课表选择
- 不新增数据库迁移

## Decisions

### D1: 新增 DAO JOIN 查询获取课表+学期名

在 `TimetableDao` 中新增 `observeAllWithSemester()` 返回 `LiveData<List<TimetableWithSemester>>`，其中 `TimetableWithSemester` 是一个 POJO，包含 timetable 所有字段 + `semesterName`。

**替代方案**: 在 Activity 中分别查询 timetables 和 semesters，在内存中合并。放弃——数据可能不同步，且多一次查询开销。

### D2: TimetableManageActivity 不再接收 EXTRA_SEMESTER_ID

Activity 改为加载全部课表，不再过滤学期。标题改为"课表管理"（不带学期后缀）。

**替代方案**: 保留 EXTRA_SEMESTER_ID 作为可选过滤器。放弃——增加复杂度，且全局列表是主要用例。

### D3: 新建课表对话框增加学期 Spinner

`promptCreate()` 弹出的对话框从单输入框改为：学期 Spinner + 课表名输入框。Spinner 数据源为 `semesterDao.observeAll()`，默认选中活跃学期。

**替代方案**: 让用户先选择学期再新建。放弃——步骤过多，Spinner 内联更流畅。

### D4: 导入默认使用活跃学期

`confirmImport()` 不再依赖传入的 `semesterId`，改为从 `SemesterRepository.getCachedOrDefault()` 获取活跃学期 ID。

**替代方案**: 导入预览时增加学期选择。放弃——导入已自动创建新课表，用户可在导入后移动课程。

### D5: SemesterManageActivity 移除课表入口

删除 `openTimetableManage()` 方法、`btnManageTimetables` 按钮及其点击监听。`item_semester_row.xml` 中移除该按钮。

### D6: SettingsActivity 管理课表入口直连

设置页"管理课表"按钮的 `startActivity` 不再传 `EXTRA_SEMESTER_ID`，直接打开全局列表。

## Risks / Trade-offs

- [全部课表列表可能很长] → 按学期分组排序（活跃学期在前），列表项紧凑显示
- [导入不再关联指定学期] → 导入使用活跃学期，符合用户预期（导入的是当前学期的课表）
- [EXTRA_SEMESTER_ID 移除是破坏性变更] → 只有一个调用方（SemesterManageActivity），同步修改即可
