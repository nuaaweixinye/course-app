## Why

学期管理目前是独立的Activity，操作路径过长。"课表"标签（timetableProfiles）只是课程上的逗号分隔字符串，无法独立管理。用户需要将学期管理整合到设置页，并支持创建多个课表——每个课表关联一个学期，课程归属于具体的课表。

## What Changes

- **BREAKING** 移除 `SemesterManageActivity`，学期管理合并到 `SettingsActivity` 的"学期"区域
- 新增 `TimetableEntity`（课表），作为独立实体关联 `semesterId`
- 课程从 `timetableProfiles` 标签改为关联 `timetableId`
- 课表管理：设置页内可新建、重命名、删除课表
- 工具栏标签芯片改为课表芯片，过滤方式不变
- 移入学期/批量操作改为移入课表

## Capabilities

### New Capabilities
- `timetable-entity`: 课表实体管理（创建、编辑、删除、切换），关联学期

### Modified Capabilities
- `course-management`: 课程关联方式从 `timetableProfiles` 标签改为 `timetableId` 外键
- `app-settings`: 设置页增加学期管理 + 课表管理UI

## Impact

- 数据库版本从 2 升级到 3，新增 `timetables` 表，`courses.timetableProfiles` 改为 `courses.timetableId`
- 移除 `SemesterManageActivity` 及相关 layout/menu 文件
- `CourseDetailActivity`、`MainActivity`、`MainViewModel`、`CourseRepository` 需适配新模型
- 迁移逻辑从 `timetableProfiles` 字符串提取为课表
