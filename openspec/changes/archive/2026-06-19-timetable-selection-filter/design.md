## Context

当前课表管理支持增删改，但首页显示学期下所有课程而非某一课表课程。用户需要在课表管理中选择"当前课表"，首页自动过滤。

`CourseRepository` 已有 `activeTimetableId` 字段和 `setActiveTimetableId()`，`MainViewModel` 也有 `activeTimetableId` MutableLiveData，但：
- 活跃课表没有持久化到数据库（`isActive` 字段不存在）
- 未选择课表时首页显示全部课程而非未分配课程
- `observeCourses()` 不按 timetableId 过滤（只过滤了 sessions）

## Goals / Non-Goals

**Goals:**
- 持久化活跃课表状态（`TimetableEntity.isActive`），每个学期一个活跃课表
- `TimetableManageActivity` 增加"设为当前"操作
- 首页按活跃课表过滤课程列表和课程节次
- 未选择课表时显示 `timetableId IS NULL` 的课程
- 切换学期后自动加载该学期的活跃课表
- 数据库 v4→v5 迁移

**Non-Goals:**
- 不在首页加课表切换器/下拉选择
- 不改批次移动对话框的逻辑
- 不改 CourseEditActivity 的课表选择器

## Decisions

### 1. 存储方式：TimetableEntity.isActive 字段
- 每个学期下最多一个课表 `isActive=1`
- `clearActive(semesterId)` + `setActive(id)` 原子操作
- 类似 SemesterEntity.isActive 模式，复用成熟设计

### 2. 首页过滤策略：MediatorLiveData 组合
- `CourseRepository.observeCourses()` 改为 `MediatorLiveData`，同时观察 Room 课程列表和活跃课表 ID
- 当活跃课表变化时，重新过滤：
  - `activeTimetableId == null` → 查询 `WHERE timetableId IS NULL AND semesterId = :semesterId`
  - `activeTimetableId != null` → 查询 `WHERE timetableId = :timetableId`
- `observeWeekSessions()` 中的 `combine()` 方法复用现有 `activeTimetableId` 过滤逻辑，追加 null=未分配

### 3. 活跃课表自动加载
- `MainViewModel` 观察 `SemesterRepository.observeActive()` 变化
- 当活跃学期变化 → 重新查询该学期的活跃课表
- 当活跃课表变化 → 更新 `CourseRepository.activeTimetableId` → 首页自动刷新

### 4. TimetableManageActivity "设为当前"
- 在课表点击对话框中插入"设为当前"选项（仅当非活跃课表时显示）
- 列表项行显示活跃状态（✓标记）
- 调用 `TimetableRepository.switchTo(id, semesterId)`
- 留在管理页，用户手动返回首页后 LiveData 自动刷新

## Risks / Trade-offs

- **[数据一致性]** 删除活跃课表时，`delete()` 会一并删除，之后 `observeActive()` 返回 null，首页自动切换到显示未分配课程
- **[迁移风险]** 添加 `isActive` 列用 `ALTER TABLE ... ADD COLUMN ... DEFAULT 0`，SQLite 原生支持，风险低
- **[UI 延迟]** 活跃课表变化后的首页过滤依赖 LiveData 传播，可能有短暂闪烁，可接受
