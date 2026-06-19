## Why

课表管理功能目前支持增删改课表，但首页仍然显示学期下所有课程，无法只显示某一课表的课程。用户需要在课表管理中选择一个课表作为"当前课表"，首页自动过滤只显示该课表的课程。

## What Changes

- 给 `TimetableEntity` 增加 `isActive` 字段（每个学期一个活跃课表）
- `TimetableManageActivity` 增加"设为当前"操作
- `MainActivity` 首页按活跃课表 ID 过滤课程
- 未选择课表时，首页只显示未分配课表的课程（`timetableId IS NULL`）
- 数据库 v4→v5 迁移添加 `isActive` 列
- 课表管理中的"设为当前"选中后不跳转，留在管理页，手动返回首页后自动刷新

## Capabilities

### New Capabilities
- `timetable-active-selection`: 课表活跃状态管理 + 首页按活跃课表筛选课程

### Modified Capabilities

<!-- None -->

## Impact

- `TimetableEntity`: 新增 `isActive` 字段
- `TimetableDao`: 新增 `clearActive`/`setActive`/`observeActive` 方法
- `TimetableRepository`: 新增 `switchTo`/`observeActive` 方法
- `MainViewModel`: 观察活跃课表，按 `timetableId` 过滤课程列表
- `MainActivity`: 首页过滤 + 显示当前课表名称
- `TimetableManageActivity`: 对话框增加"设为当前"入口
- `AppDatabase`: 新增 `MIGRATION_4_5`
- `CourseDao`/`CourseRepository`: 可能需要支持按活跃课表过滤的查询
- **Non-goals**: 不在首页加课表切换器；不改批次移动对话框逻辑
