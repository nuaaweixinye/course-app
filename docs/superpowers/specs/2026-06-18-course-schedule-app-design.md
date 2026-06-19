# 课表 App 设计文档

**日期**: 2026-06-18
**项目**: course shedule (com.courseshedule)
**状态**: 设计已确认，待实现

---

## 1. 目标与范围

为大学生打造一款 Android 课表应用，支持周课表展示、手动管理课程、文件导入、桌面小组件、作业/考试备忘，以及周次例外（调休/停课）。

### v1 范围（本次实现）

**核心功能**
- 周课表展示（7 列网格 × 节次行），支持周次切换
- 手动添加/编辑/删除课程（一门课可多个时段）
- 本地持久化存储
- 文件导入课表（.csv / .xlsx / .ics）
- 教务系统导入接口预留（v2 实现）

**附加功能**
- 桌面小组件（显示今日接下来的课）
- 作业/考试备忘（独立于课表）
- 周次例外 / 调休（单双周、停课、调换）
- 深色模式（跟随系统）

### 明确不做（v1）
- 上课前通知提醒（v2 考虑）
- 教务系统实际抓取（仅预留接口）
- 云端同步

---

## 2. 技术栈

| 维度 | 选型 | 说明 |
|---|---|---|
| 语言 | Java 11 | 沿用项目现有配置 |
| UI 框架 | XML 布局 + Material 3 组件 | 已有 `material:1.13.0` |
| 视图绑定 | ViewBinding | 启用，替代 findViewById |
| 架构 | MVVM | Model-View-ViewModel |
| 数据库 | Room | SQLite 抽象层 |
| 异步/观察 | ViewModel + LiveData | 生命周期感知 |
| 数据层 | Repository 模式 | 单一数据来源 |
| 导航 | 多 Activity | 每屏一个 Activity |
| 依赖注入 | 手动构造 | 不引入 Hilt，保持简单 |
| 小组件 | AppWidgetProvider + RemoteViews | 桌面 4×2 |

### 需新增依赖（gradle/libs.versions.toml）

```
room-runtime, room-compiler (annotationProcessor)
lifecycle-viewmodel, lifecycle-livedata
```
ViewBinding 在 `app/build.gradle.kts` 的 `android{}` 块中启用 `buildFeatures { viewBinding = true }`。

---

## 3. 数据模型

### 3.1 实体关系

```
Course 1───* CourseSession 1───* SessionException
                                         (可选，按需创建)

Course 1───* Task              (Task.courseId 可空，可不关联)

SemesterConfig                (单例，应用级配置)
```

### 3.2 表结构

#### Course（课程）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | long (PK, autogen) | 主键 |
| name | String | 课程名，如"高等数学" |
| teacher | String | 教师姓名 |
| colorTag | int | 颜色索引，映射到侧边条颜色色板 |
| note | String | 备注 |

#### CourseSession（课次 — 一门课可多个时段）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | long (PK, autogen) | 主键 |
| courseId | long (FK → Course) | 所属课程，级联删除 |
| dayOfWeek | int | 1=周一 … 7=周日 |
| startPeriod | int | 起始节次 (1-12) |
| endPeriod | int | 结束节次 (≥ startPeriod) |
| location | String | 教室，如"教三301" |
| weekPattern | String | 周次模式：`"1-16"` 或 `"1,3,5,7,9,11,13,15"`(单周) 或 `"1-15"` |

#### SessionException（周次例外 — 调休/停课）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | long (PK, autogen) | 主键 |
| sessionId | long (FK → CourseSession) | 所属课次 |
| weekNo | int | 例外发生的周次 |
| type | int | `CANCEL=0` 停课 / `MOVED=1` 调换 |
| moveToDayOfWeek | Integer | 仅 MOVED 用，调到周几；CANCEL 时为 null |

#### Task（作业/考试备忘）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | long (PK, autogen) | 主键 |
| title | String | 标题 |
| type | int | `HOMEWORK=0` / `EXAM=1` |
| courseId | Long (FK → Course, nullable) | 可选关联课程 |
| dueDate | long | 截止时间 (epoch millis) |
| done | boolean | 是否完成 |
| note | String | 备注 |

#### SemesterConfig（学期配置 — 单例，单行表）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | long (固定 = 1) | 单例标识 |
| startDate | long | 开学第一周的周一 (epoch millis, 0:00) |
| totalWeeks | int | 学期总周数，默认 16 |
| periodTimesJson | String | 每节课起止时间的 JSON 数组 |

### 3.3 节次时间表（默认 12 节）

存储为 JSON，默认值如下（学校不同可改）：
```json
[
  {"start":"08:00","end":"08:45"},
  {"start":"08:55","end":"09:40"},
  {"start":"10:00","end":"10:45"},
  {"start":"10:55","end":"11:40"},
  {"start":"14:00","end":"14:45"},
  {"start":"14:55","end":"15:40"},
  {"start":"16:00","end":"16:45"},
  {"start":"16:55","end":"17:40"},
  {"start":"19:00","end":"19:45"},
  {"start":"19:55","end":"20:40"},
  {"start":"20:50","end":"21:35"},
  {"start":"21:45","end":"22:30"}
]
```
上午 4 节 + 下午 4 节 + 晚上 4 节。午休落在第 4-5 节之间，晚饭落在第 8-9 节之间。

### 3.4 颜色色板（colorTag 索引）

固定 8 色循环分配，用于课程卡片左侧条：
```
0=#6366f1 靛蓝    1=#10b981 翠绿    2=#f59e0b 琥珀    3=#ef4444 红
4=#8b5cf6 紫      5=#06b6d4 青      6=#ec4899 粉      7=#84cc16 黄绿
```
深色模式下同色板自动提亮（通过 Material 主题 `colorOnSurface` 变体）。

---

## 4. 屏幕 & 导航

### 4.1 屏幕清单

| Activity | 作用 | 入口 |
|---|---|---|
| `MainActivity` | 周课表网格，承载"课表"Tab | 启动屏 |
| `TaskListActivity` | 作业/考试列表，承载"任务"Tab | 底部 Tab |
| `SettingsActivity` | 学期/节次/主题/备份，承载"设置"Tab | 底部 Tab |
| `CourseDetailActivity` | 课程详情 + 所有时段 | 点课程卡片 |
| `CourseEditActivity` | 添加/编辑课程表单 | FAB / 详情页编辑 |
| `ImportActivity` | 文件导入：选文件→解析→预览→确认 | FAB 展开项 |

### 4.2 主界面结构（MainActivity）

```
┌─────────────────────────────────────┐
│ TopAppBar                           │
│   第 3 周 · 本周      [今] [⚙]      │
├─────────────────────────────────────┤
│ WeekSwitcher                        │
│   ‹  第 3 周 / 16  ›                 │
├─────────────────────────────────────┤
│ DayHeader (当天列高亮)               │
│   节  一  二 [三] 四  五  六  日     │
├─────────────────────────────────────┤
│ TimetableView (7 列 × 12 行 网格)    │
│   - 课程卡：白底 + 左侧彩色条        │
│   - 跨节课程：竖向合并显示           │
│   - 当前节次：描边/底色高亮          │
│   - 午休/晚饭：虚线分隔              │
│   - 周末列：无课时折叠               │
├─────────────────────────────────────┤
│                       (FAB 展开)    │
│                    ┌─────┐ ┌─────┐  │
│                    │导入 │ │添加 │  │
│                    └─────┘ └─────┘  │
├─────────────────────────────────────┤
│ BottomNavigation                    │
│   [📇 课表]  [✓ 任务]  [⚙ 设置]      │
└─────────────────────────────────────┘
```

**网格实现**：自定义 `TimetableView`（基于 `RecyclerView` 或绝对定位的 `ViewGroup`）。课程卡用 `absoluteLayout`/`ConstraintLayout` 按 `startPeriod`/`endPeriod` 计算垂直偏移，支持跨节竖向延伸。

### 4.3 课程编辑（CourseEditActivity）

- 课程基本信息：名称、教师、颜色选择（8 色板）、备注
- 时段列表（可增删多个）：
  - 周几（1-7）
  - 起止节次（节次选择器）
  - 教室
  - 周次模式：预设选项（全周 1-16 / 单周 / 双周 / 自定义勾选 1-16）
- 保存校验：名称非空、节次合法、周次模式非空

### 4.4 导入流程（ImportActivity）

```
选文件 (.csv/.xlsx/.ics)
    ↓
解析 (后台线程)
    ↓
字段映射 / 格式识别
    ↓
预览界面（列表显示识别出的课程 + 时段，可勾选/编辑）
    ↓
确认 → 写入数据库（事务：可选清空旧数据 / 追加）
```

**支持的文件格式**：
- **CSV**: 表头识别列名（课程名/教师/周几/节次/教室/周次），兼容教务系统常见导出
- **XLSX**: 用 Apache POI 或简化库解析（评估依赖体积）
- **ICS**: 标准 iCalendar VEVENT，`RRULE` 解析周次，`DTSTART` 映射到节次时间表

---

## 5. 导入接口设计（v2 预留）

```java
public interface TimetableImporter {
    /** 各校实现：登录 + 抓取 + 解析，返回结构化课程 */
    List<ParsedCourse> fetch(ImportCallback callback) throws ImportException;
}

public interface ImportCallback {
    void onProgress(int current, int total);
}
```

v2 实现具体学校的 `XxxUniversityImporter`，结果走与文件导入相同的预览确认流程，保证 UX 一致。

---

## 6. 关键算法

### 6.1 当前周计算
```java
int currentWeek = (int) ((todayMillis - semesterStartMillis) / WEEK_MILLIS) + 1;
// 越界处理：< 1 → 1, > totalWeeks → totalWeeks
```

### 6.2 当前节次匹配
遍历 `periodTimes`，找当前时间落在哪个 `[start, end]` 区间；若在两节之间，返回下一节作为"即将开始"。

### 6.3 某周某天的课程渲染
```java
for (CourseSession s : sessionsOfDay) {
    if (matchesWeek(s.weekPattern, weekNo)) {
        // 检查例外
        SessionException ex = findException(s.id, weekNo);
        if (ex == null) render(s);                      // 正常渲染
        else if (ex.type == CANCEL) skip;               // 停课
        else if (ex.type == MOVED) moveTo(ex.dayOfWeek);// 调换到其他天
    }
}
```

### 6.4 周次模式匹配
```java
boolean matchesWeek(String pattern, int weekNo) {
    // "1-16"  → 1..16
    // "1,3,5" → 枚举
    // 解析一次后缓存为 Set<Integer> 或区间
}
```

### 6.5 Widget 数据查询
查询今日（应用了当前周 + 例外）的课次，过滤 `endPeriod 时间 > now` 的，取前 2-3 条显示。

---

## 7. 主题与视觉

### 7.1 浅色模式（默认）
- 背景：`#fafafa` / `#ffffff`
- 卡片：白底 `#ffffff` + 1px 边框 `#f3f4f6` + 左侧 3dp 彩色条
- 主文字：`#1f2937`，次文字：`#6b7280`，弱文字：`#9ca3af`
- 强调色（当天列、FAB）：`#4f46e5`

### 7.2 深色模式（跟随系统）
- 背景：`#1f2937` / `#374151`
- 卡片：`#374151` + 左侧彩色条（同色板，自动适配）
- 主文字：`#f9fafb`，次文字：`#d1d5db`
- 通过 Material 3 主题 + `values-night` 资源实现

### 7.3 Material 3 主题
- 基于 `Theme.Material3.DayNight`
- 主色调靛蓝（`#4f46e5`），作为 FAB、Tab 选中、当天高亮色
- 不启用 Material You 动态配色（保持品牌一致；可作设置项后续加）

---

## 8. 项目结构

```
com.courseshedule/
├── data/
│   ├── local/
│   │   ├── AppDatabase.java          (Room)
│   │   ├── dao/  (CourseDao, CourseSessionDao, TaskDao, ...)
│   │   └── entity/  (CourseEntity, CourseSessionEntity, ...)
│   ├── model/  (领域模型: Course, CourseSession, Task, ...)
│   ├── repository/  (CourseRepository, TaskRepository, ConfigRepository)
│   └── import/  (FileImporter, CsvParser, IcsParser, TimetableImporter 接口)
├── ui/
│   ├── main/  (MainActivity, MainViewModel, TimetableView, CourseCardAdapter)
│   ├── task/  (TaskListActivity, TaskListViewModel, TaskAdapter)
│   ├── course/  (CourseDetailActivity, CourseEditActivity, CourseEditViewModel)
│   ├── import/  (ImportActivity, ImportViewModel, ImportPreviewAdapter)
│   ├── settings/  (SettingsActivity, SettingsViewModel)
│   └── common/  (ColorPalette, PeriodUtils, WeekUtils, base classes)
├── widget/  (TodayWidgetProvider, TodayWidgetService, TodayWidgetFactory)
└── App.java  (Application, 初始化 DB 默认数据)
```

---

## 9. 测试策略

### 9.1 单元测试（JUnit，`app/src/test`）
- `WeekUtils`: 周次计算、weekPattern 解析与匹配
- `PeriodUtils`: 节次时间匹配、当前节次查找
- `CsvParser` / `IcsParser`: 各格式解析（含异常文件）
- Repository 逻辑（用 in-memory Room DB）

### 9.2 仪器测试（Espresso，`app/src/androidTest`）
- 主界面：网格渲染、周次切换、点卡片跳详情
- 课程编辑：表单校验、保存后列表更新
- 导入：选文件→预览→确认端到端
- Widget：渲染（Robolectric 或仪器测试）

---

## 10. 风险与待决

| 项 | 风险 | 应对 |
|---|---|---|
| 网格视图复杂度 | 自定义 TimetableView 工作量大 | 先用简化版（固定行高 RecyclerView），跨节合并可后置 |
| XLSX 解析依赖体积 | Apache POI 很重 | 优先 CSV/ICS；XLSX 用轻量库或仅支持简单表格 |
| Widget RemoteViews 限制 | 不能用自定义 View | 用 `ListView` + `RemoteViewsService`，布局保持简单 |
| 教务系统多样性 | 各校不同 | v1 只做接口，不实现；避免过度设计 |

---

## 11. 实现优先级（建议迭代顺序）

1. **基础设施**：依赖配置、Room DB、entities/DAOs、Application 初始化
2. **数据层**：Repository、WeekUtils/PeriodUtils + 单测
3. **主界面骨架**：MainActivity + 7×12 网格（先无跨节合并）+ 周次切换
4. **课程管理**：CourseEdit + CourseDetail + CRUD 打通
5. **导入**：CSV 先行，ICS 次之，XLSX 最后
6. **设置 + 学期配置**：开学日期、节次时间表、深色模式开关
7. **任务列表**：TaskList + 添加/完成
8. **周次例外**：调休/停课的编辑入口与渲染
9. **桌面小组件**：今日课程
10. **打磨**：跨节合并、当天/当前节次高亮、空状态、深色模式校准
