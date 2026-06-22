## Why

课表切换存在多个 bug 和 UX 问题：设置活跃课表后首页不刷新（课程列表不变）；在非活跃学期创建的课表不显示；首页在无课表时仍显示未分配课程而非空状态提示。根因是课表与学期的联动逻辑断裂——切换课表时未同步切换学期，且 WeekSession MediatorLiveData 未观察活跃课表变化。

## What Changes

- **课表切换联动学期**：`TimetableRepository.switchTo()` 同时切换活跃学期为课表所属学期
- **首页刷新修复**：WeekSession MediatorLiveData 增加对活跃课表变化的观察，确保切换课表后 `combine()` 重新执行
- **首页空状态**：无活跃课表时显示"没有课表"空提示，不再显示未分配课程
- **移除独立学期切换**：移除 SemesterManageActivity 的"设为当前学期"功能，学期只能通过课表切换间接更改
- **创建课表守卫**：无学期时提示先创建学期

## Non-goals

- 不改变数据库 schema（无新增迁移）
- 不修改课程编辑中的课表选择逻辑
- 不修改导入逻辑

## Capabilities

### New Capabilities
_(无)_

### Modified Capabilities
- `timetable-entity`: 课表切换同时切换活跃学期；移除独立学期切换入口
- `timetable`: 首页无活跃课表时显示空状态而非未分配课程
- `app-settings`: 创建课表前检查学期是否存在

## Impact

- `TimetableRepository.java`: `switchTo()` 增加切换学期逻辑
- `SemesterRepository.java`: 新增 `setActive()` 方法
- `MainViewModel.java`: 学期观察改为由课表切换驱动；WeekSession 增加课表源观察
- `CourseRepository.java`: `observeWeekSessions()` MediatorLiveData 增加活跃课表源
- `TimetableManageActivity.java`: 移除独立学期切换入口
- `SemesterManageActivity.java`: 移除"设为当前学期"选项
- `MainActivity.java`: 空状态显示"没有课表"
