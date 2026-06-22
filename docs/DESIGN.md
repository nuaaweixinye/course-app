# CourseShedule 设计文档

## 架构概览

```
┌─────────────────────────────────────────────────────────────┐
│                        Activity 层                          │
│  MainActivity  CourseEditActivity  TimetableManageActivity  │
│  SettingsActivity  TaskListActivity  CourseDetailActivity   │
│  TimetableCourseListActivity  ImportActivity                │
│  TodayWidgetFactory                                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ LifecycleOwner / observe
┌──────────────────────────▼──────────────────────────────────┐
│                       ViewModel 层                          │
│  MainViewModel  CourseEditViewModel                         │
└──────────────────────────┬──────────────────────────────────┘
                           │ LiveData / callback
┌──────────────────────────▼──────────────────────────────────┐
│                     Repository 层                           │
│  CourseRepository  SemesterRepository  TimetableRepository  │
└──────────────────────────┬──────────────────────────────────┘
                           │ Room DAO
┌──────────────────────────▼──────────────────────────────────┐
│                      Room Database                          │
│  CourseDao  CourseSessionDao  SemesterDao  TimetableDao     │
│  SessionExceptionDao  TaskDao                               │
└─────────────────────────────────────────────────────────────┘
```

- **无 DI 框架**：Repository 和 ViewModel 均通过手动构造（`new`），数据库单例通过 `App.getDatabase()` 获取。
- **数据库版本**：当前版本 7，包含 6 次迁移（MIGRATION_1_2 ~ MIGRATION_6_7）。
- **多 Activity**：每个页面各自是一个 Activity，不采用 Fragment 或 Navigation Component。

---

## 数据层

### Room 实体

| 表 | 实体 | 用途 | FK 约束 |
|---|---|---|---|
| `semesters` | `SemesterEntity` | 学期（名称、开学日期、总周数、节次配置） | — |
| `timetables` | `TimetableEntity` | 课表（名称、归属学期、是否活跃） | `semesterId → semesters(id) CASCADE` (v7) |
| `courses` | `CourseEntity` | 课程（名称、教师、备注、颜色标签、归属课表） | `timetableId → timetables(id) CASCADE` (v6) |
| `course_sessions` | `CourseSessionEntity` | 课次（星期几、起止节次、周次 pattern、地点） | `courseId → courses(id) CASCADE` |
| `session_exceptions` | `SessionExceptionEntity` | 课次例外（调停课记录） | `sessionId → course_sessions(id) CASCADE` |
| `tasks` | `TaskEntity` | 任务（待办事项） | — |

### 外键级联链

```
DELETE 学期
  → CASCADE → 课表
    → CASCADE → 课程
      → CASCADE → 课程时段
        → CASCADE → 调停课
```

删除任一上游实体，下游数据全部自动清除，不会产生孤儿行。

### 核心 DAO

**`TimetableDao`** 的关键方法：

```sql
-- 全局活跃课表（跨学期唯一）
SELECT * FROM timetables WHERE isActive = 1 LIMIT 1
```

- `isActive` 列通过 `clearAllActive()` + `setActive(id)` 维护全局唯一性，并用 `runInTransaction` 保证原子性。
- `observeActiveGlobal()` 返回 LiveData，ViewModel 通过 `observeForever` 缓存值。

### 数据流：课程显示

```
WeekPickerDialog / etWeek
        │ setSelectedWeek(week)
        ▼
MainViewModel.selectedWeek (MutableLiveData<Integer>)
        │ Transformations.switchMap
        ▼
CourseRepository.observeWeekSessions(week) (MediatorLiveData)
        │ 合并 3 个数据源：
        │   sessionDao.observeAllWithCourse()
        │   exceptionDao.observeAll()
        │   activeTimetableId (MutableLiveData<Long>)
        │ 过滤条件：
        │   activeSemesterId 匹配
        │   WeekUtils.matchesWeek() 匹配
        │   timetableId == activeTimetableId (或 null == null)
        ▼
MainActivity.renderGrid()
```

### 活跃课表/学期状态同步

```
User clicks "设为当前课表"
        │ TimetableRepository.switchTo(id, semesterId)
        ▼
db.runInTransaction {
    dao.clearAllActive()         -- timetables
    dao.setActive(id)
    semesterDao.clearActive()    -- semesters
    semesterDao.setActive(semesterId)
}
        │
        ▼
Room invalidates LiveData → observeActiveGlobal() 推送
        │
        ▼
MainViewModel.ttObserver → activeTimetable.setValue(tt)
                         → courseRepository.setActiveTimetableId(tt.id)
                         → activeSemesterSource 推送新学期
                         → onDbSemesterChanged() 字段变更检测
```

---

## ViewModel 层

### MainViewModel

| 观察源 | 类型 | 用途 |
|---|---|---|
| `timetableRepository.observeActiveGlobal()` | `observeForever` | 缓存全局活跃课表 |
| `semesterRepository.observeActive()` | `observeForever` | 缓存活跃学期，含字段变更检测 |
| `selectedWeek` | `switchMap` → `observeWeekSessions` | 周次变化时重新加载课程 |

`hasActiveTimetable()` 返回 `activeTimetable.getValue() != null`，纯缓存访问，不走 DB。

### 字段变更检测 (`onDbSemesterChanged`)

```java
boolean idChanged = current == null || sem.id != current.id;
boolean fieldsChanged = current != null && (
    sem.startDate != current.startDate ||
    sem.totalWeeks != current.totalWeeks ||
    !Objects.equals(sem.periodTimesJson, current.periodTimesJson));
if (idChanged || fieldsChanged) {
    onSemesterChanged(sem);
}
```

避免无关字段变化（如 `name`）触发不必要的重加载。

### CourseEditViewModel

| 方法 | 用途 |
|---|---|
| `initCreate(defaultColor)` | 新课程，从全部课程中加载作为参考 |
| `initEdit(courseId)` | 编辑已有课程，加载课程+课次 |
| `save(course, sessions, onSaved)` | 校验 name/timetableId/sessions → 调 save/update → 回调 |

`save()` 新增 `timetableId == null` 防御性校验，返回 `R.string.err_no_timetable`。

---

## Activity 层

### MainActivity

**toolbar "+" 按钮（三层防护）**：

```
User clicks "+"
        │
        ▼
try {
    viewModel.hasActiveTimetable()  ← 缓存层（无 DB）
        │ true → startActivity(CourseEditActivity)
        │ false → startActivity(TimetableManageActivity) + Toast
} catch (Exception e) {
    startActivity(TimetableManageActivity) + Toast  ← 兜底层
}
```

**周次选择**：

- `etWeek` 设 `focusable=false`，点击弹出 `AlertDialog.setItems()` 周次列表（1..totalWeeks，当前周标注"（本周）"）。
- 仍支持手动输入（`OnEditorActionListener` + `applyWeekInput`）。

**事件**：

| 事件 | 行为 |
|---|---|
| 返回 onCreate | Room LiveData 自动刷新（无需 onResume） |
| "+" 无活跃课表 | 跳转 TimetableManageActivity |
| "+" 有活跃课表 | 跳转 CourseEditActivity |
| 周次选择 | showWeekPickerDialog → AlertDialog → setSelectedWeek |
| batch move | 查询活跃学期课表 → AlertDialog 选择目标 → batchMoveCourses |
| batch delete | 确认对话框 → batchDeleteCourses |

### CourseEditActivity

**课表选择器逻辑**：

Spinner 仅展示有效课表（移除了"无课表"选项），优先级：

1. `currentTtId`（编辑模式，已有课程的课表）
2. `extraTimetableId`（从 TimetableManageActivity 传入）
3. `globalActive` 全局活跃课表
4. 无课表 → 隐藏 Spinner，显示 "请先创建课表"

**空课表守卫**：

```java
if (timetableList.isEmpty()) {
    // 隐藏 Spinner，显示提示文字
    // timetableNoticeAdded 标志防止重复添加
    return;  // onSave 也会因 ttPos < 0 阻止保存
}
```

**周次预设**：

| 预设 | index | weekPattern |
|---|---|---|
| 全部周 | 0 | `allWeeks` |
| 单周 | 1 | `oddWeeks` |
| 双周 | 2 | `evenWeeks` |
| 自选周次 | 3 (WEEK_CUSTOM) | `WeekPickerDialog` 自定义（4列网格） |

`WeekPickerDialog`：弹窗显示 4 列周次按钮，支持多选，确认后拼接为自定义 weekPattern。

**保存流程**：

```java
private void onSave() {
    int ttPos = binding.spinTimetable.getSelectedItemPosition();
    if (ttPos < 0 || timetableList.isEmpty()) { /* 拒绝 */ return; }
    CourseEntity course = new CourseEntity();
    // ...
    course.timetableId = timetableList.get(ttPos).id;    // 始终非空
    course.semesterId = timetableList.get(ttPos).semesterId;
    // ...
    viewModel.save(course, sessions, onSaved -> finish());  // 等待回调
}
```

### TimetableManageActivity

**对话框选项**：

| 活跃课表 | 非活跃课表 |
|---|---|
| 添加课程 | 设为当前 |
| 查看课程 | 添加课程 |
| 重命名 | 查看课程 |
| 删除 | 重命名 |
| | 删除 |

**"添加课程" 入口**：传入 `EXTRA_TIMETABLE_ID` 启动 `CourseEditActivity`，`onLoaded` 按该课表的 `semesterId` 查询课表列表。

**导入流程**：选目标课表（已有课表 / "创建新课表"）→ 文件选择器 → 解析预览 → 确认导入。使用 `pendingImportTimetableId` 记录用户选中的课表。

### ImportActivity

**独立文件导入**（文件分享/打开入口）：

- 选择文件 → 自动检测格式（HTML/XLS/CSV/ICS）→ 解析 → 预览 → 确认
- `onConfirm()` 从**全局活跃课表**获取 `timetableId` 和 `semesterId`，保证导入的课程始终有归属

---

## 关键设计决策

### 1. 活跃课表全局唯一（而非按学期）

**问题**：课程编辑时需要显示课表选择器，按学期过滤后用户看不到其他学期的课表。

**决策**：`timetables` 表加 `isActive` 列，全局唯一。`clearAllActive()` 清除所有，`setActive(id)` 设置一个。

**优势**：
- `getActiveGlobal()` 一条查询即可
- `MainViewModel` 单一观察源 `observeActiveGlobal()`
- CourseEditActivity 无需跨学期查询

### 2. 默认学期不再自动创建

**问题**：`SemesterRepository.getSeedingDefault()` 在首次访问时自动写入数据库，导致空状态体验不好。

**决策**：移除自动创建。`getCachedOrDefault()` 只返回 transient 对象（id=0），不写库。无学期时 UI 显示空状态。

```java
public SemesterEntity getCachedOrDefault() {
    SemesterEntity c = cachedActive;
    if (c != null) return c;
    SemesterEntity def = new SemesterEntity();  // transient, id=0
    def.name = "默认学期";
    def.startDate = PeriodUtils.mondayOfDay(System.currentTimeMillis());
    def.totalWeeks = 16;
    def.periodTimesJson = PeriodUtils.DEFAULT_PERIOD_TIMES_JSON;
    def.isActive = true;
    return def;
}
```

### 3. 启动修复重复活跃学期

`App.onCreate()` 调用 `SemesterRepository.repairDuplicateActive()`：

- 清理多个活跃学期
- 如果活跃学期存在但无活跃课表，自动激活第一个课表

### 4. 数据库事务保护

所有 switchTo 操作（学期切换、课表切换）都使用 `runInTransaction`，保证 clear+set 的原子性：

```java
db.runInTransaction(() -> {
    dao.clearAllActive();
    dao.setActive(id);
    semesterDao.clearActive();
    semesterDao.setActive(semesterId);
});
```

### 5. Room LiveData 自动刷新

`observeWeekSessions` 返回 `MediatorLiveData`，合并 3 个 Room 数据源。Room 在相关表变更时自动推送更新，无需手动调用 `onResume` 或 `notifyDataSetChanged`。

### 6. 新建课表不自动激活

新建课表时不再自动调用 `clearAllActive()` + `setActive()`，需要用户手动"设为当前课表"。避免新建课表造成正在使用的课表状态丢失。

### 7. 外键级联删除

三层级联：

| 层级 | FK | 作用 |
|---|---|---|
| `timetables.semesterId → semesters.id` (v7) | CASCADE | 删学期→删课表 |
| `courses.timetableId → timetables.id` (v6) | CASCADE | 删课表→删课程 |
| `course_sessions.courseId → courses.id` | CASCADE | 删课程→删时段 |
| `session_exceptions.sessionId → course_sessions.id` | CASCADE | 删时段→删例外 |

保证不会产生孤儿数据。

---

## 数据完整性保障

### 防御性校验（三层保护）

| 层级 | 文件 | 检查 |
|---|---|---|
| UI | `CourseEditActivity.onSave()` | `ttPos < 0` 或无课表时拒绝保存 |
| ViewModel | `CourseEditViewModel.save()` | `timetableId == null` 返回错误码 |
| Repository | `CourseRepository.saveCourse()` | `timetableId == null` 抛 IllegalArgumentException |
| DB | `@ForeignKey` | Room/SQLite 级 + CASCADE 保护 |

### 课程必须关联课表

- Spinner 移除了"无课表"选项，用户必选一个有效课表
- 导入流程（`ImportActivity` / `TimetableManageActivity`）均从活跃课表或用户指定课表获取 timetableId
- `course.timetableId` 始终为非空 `Long`（但保持 `Long` 可空以兼容降级路径）

---

## 风险防范

### 主线程数据库查询

**问题**：Room 禁止在主线程执行同步查询。

**解决**：
- 所有写入（insert/update/delete）通过 `ExecutorService.io.execute()` 在后台线程执行
- 读取优先使用 LiveData（Room 自动后台查询）
- 同步读取仅在后台线程使用（如 `onLoaded` 中的 `new Thread`）
- 前台需要"是否有活跃课表"的判断，走 ViewModel 缓存 `activeTimetable.getValue()`

### 课表/学期一致性

**问题**：`extraTimetableId` 可能与全局活跃课表不在同一学期，导致 Spinner 找不到该课表。

**解决**：
```java
if (extraTimetableId != null) {
    TimetableEntity extraTt = db.timetableDao().getById(extraTimetableId);
    if (extraTt != null) {
        semId = extraTt.semesterId;
    }
}
```

### 空课表重复提示

**问题**：`onLoaded` 被多次调用时，空课表的提示文字可能重复添加。

**解决**：`timetableNoticeAdded` 标志位防止重复。

### 活跃课表竞态

**问题**：删除活跃课表后，`hasActiveTimetable()` 可能在 LiveData 推送前返回旧值。

**解决**：三层防护（try → 缓存 → 兜底），catch 块直接跳转课表管理页。

---

## 迁移策略

当前版本 7，包含 6 次迁移：

| 迁移 | 版本 | 内容 |
|---|---|---|
| MIGRATION_1_2 | 1→2 | 单行配置表 → 多行 semesters；courses 增加 semesterId |
| MIGRATION_2_3 | 2→3 | 新建 timetables；解析旧 timetableProfiles |
| MIGRATION_3_4 | 3→4 | 重建 timetables/courses（清理遗留列） |
| MIGRATION_4_5 | 4→5 | timetables 增加 isActive |
| MIGRATION_5_6 | 5→6 | courses 增加 FK CASCADE → timetables |
| MIGRATION_6_7 | 6→7 | timetables 增加 FK CASCADE → semesters |

如需新增字段或表：

1. 在 `@Entity` 中添加字段，用 `@NonNull` 或 `@Nullable` 控制非空约束
2. 新增 `@Database` 版本号 + `Migration` 实现
3. `ALTER TABLE ADD COLUMN` 推荐用 `try-catch` 包装（兼容已迁移的数据库）
4. 添加 `@ForeignKey` 时需重建表（SQLite 不支持 ADD FOREIGN KEY）

---

## 测试策略

### 单元测试
- `WeekUtils` 周次模式匹配
- `DisplaySession` 例外合并逻辑
- 纯逻辑，无需 Room

### 仪器测试
- Room inMemory + `allowMainThreadQueries`
- `CourseRepository`：CRUD、过滤、batch move/delete
- `TimetableRepository`：switchTo、create、delete
- `SemesterRepository`：repairDuplicateActive
- `IntegrationTest`：端到端学期→课表→课程→时段→例外

### 约束
- `setActiveSemester/setActiveTimetableId` 必须在 `runOnMainSync` 中调用
- LiveData 测试用 `observeForever` + `CountDownLatch`
