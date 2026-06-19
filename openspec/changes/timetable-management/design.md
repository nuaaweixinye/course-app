## Context

当前课程关联"课表"的方式是 `CourseEntity.timetableProfiles` 字符串（逗号分隔标签）。用户需要真正独立的课表实体：一个学期下可以创建多个课表，课程数据归属于某个课表。同时学期管理要从独立 Activity 合并到设置页。

## Goals / Non-Goals

**Goals:**
- `TimetableEntity` 独立实体（id, name, semesterId），1:N 与 Semester
- `CourseEntity.timetableProfiles` 改为 `timetableId`（可为 null）
- 设置页整合学期编辑 + 课表管理（CRUD + 切换）
- 工具栏标签芯片改为课表芯片，逻辑保持不变
- 数据库迁移 v2→v3（新增 timetables 表 + 列变更）
- 移除 `SemesterManageActivity` 及相关资源

**Non-Goals:**
- 课表权限/共享
- 课表间复制课程（仅移入）
- UI 动效

## Decisions

1. **TimetableEntity 设计**：`id`(LONG PK), `name`(TEXT), `semesterId`(LONG FK → semesters.id)，无其他字段。简单轻量。
2. **兼容迁移**：v2→v3 创建 timetables 表，为每个学期创建一个默认课表（名称"默认课表"），将 courses.timetableProfiles 转为 timetableId。原 timetableProfiles 非空时，生成同名课表并映射；为空则关联默认课表。
3. **设置页整合**：SettingsActivity 增加"学期管理"CardView（点击→编辑当前学期/选择学期），下方"课表管理"CardView（列出课表，可增删改切）。不再有 SemesterManageActivity。
4. **工具栏芯片**：`MainViewModel` 读取当前学期下的课表列表，芯片数量等于课表数 + "全部"芯片。点击芯片过滤 `courseRepository.setActiveTimetable(id)`。
5. **批量操作**：移入弹窗从选学期改为选课表（当前学期下的课表列表）。

## Risks / Trade-offs

- [数据库迁移复杂度] v2→v3 需要处理 timetableProfiles→timetableId 的映射；若存在多标签逗号分隔，只取第一个作为课表名 → 可接受，用户可自行调整
- [设置页膨胀] 学期+课表都放在设置页可能导致页面过长 → 使用 MaterialCardView 分组 + 折叠
- [现有数据兼容] timetableProfiles 为 null 的课程关联到默认课表 → 用户无感知
