## Context

当前课表切换逻辑存在断裂。`TimetableRepository.switchTo(id, semesterId)` 只更新课表的 `isActive` 字段，不切换活跃学期。`MainViewModel` 通过 `observeActive(semesterId)` 观察当前学期的活跃课表，但如果活跃学期未改变，切换到其他学期的课表不会生效。

`CourseRepository.observeWeekSessions()` 的 `combine()` 方法读取 `activeTimetableId` 进行过滤，但 MediatorLiveData 只观察 session 和 exception 数据源，不观察 `activeTimetableId` 变化。因此切换课表后 `combine()` 不会重新执行。

## Goals / Non-Goals

**Goals:**
- 切换活跃课表时自动切换活跃学期（课表→学期联动）
- 切换课表后首页课程列表立即刷新
- 无活跃课表时首页显示"没有课表"空状态
- 移除独立学期切换入口

**Non-Goals:**
- 不修改数据库 schema
- 不修改导入逻辑
- 不修改课程编辑页

## Decisions

### D1: switchTo 同时切换学期

`TimetableRepository.switchTo()` 在 `clearActive` + `setActive` 之外，调用 `SemesterDao.clearActive()` + `SemesterDao.setActive(semesterId)`。

**替代方案**: 让 MainViewModel 监听课表变化后手动切换学期。放弃——因为 Repository 层更可靠，且避免 ViewModel 中复杂的观察者链。

### D2: WeekSession 增加课表数据源

`CourseRepository.observeWeekSessions()` 目前只观察 session + exception。新增对 `activeTimetableId` 变化的响应：在 `CourseRepository` 中暴露一个 `activeTimetableId` 的 LiveData，MediatorLiveData 增加该源。

**替代方案**: 让 MainViewModel 在课表变化时手动触发 `selectedWeek.setValue(week)` 强制 switchMap 重建。放弃——hack 方式，不稳定。

### D3: 空状态显示

当 `activeTimetableName` 为 null 时，`renderGrid()` 显示"没有课表"提示，不显示课程。

### D4: 移除独立学期切换

从 `SemesterManageActivity` 对话框中移除"设为当前学期"选项。学期切换只通过课表切换间接完成。

## Risks / Trade-offs

- [移除学期切换入口后用户可能困惑] → 课表管理中"设为当前"会自动切换学期，UX 清晰
- [WeekSession 增加 MediatorLiveData 源可能增加开销] → 仅一个额外 Boolean/Long 源，开销可忽略
