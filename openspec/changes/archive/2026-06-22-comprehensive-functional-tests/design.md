## Context

现有测试共 6 个单元测试 + 2 个仪器测试文件，覆盖工具类和 CourseRepository 基础 CRUD。核心业务逻辑（课表切换联动、唯一活跃约束、课程过滤、批量操作、DAO JOIN 查询）无测试覆盖。测试基础设施已具备：Room inMemory + JUnit4 + AndroidJUnit4。

## Goals / Non-Goals

**Goals:**
- 覆盖 `TimetableRepository.switchTo` 的学期联动 + 全局唯一活跃
- 覆盖 `CourseRepository.observeWeekSessions` 的过滤逻辑（学期/周次/课表/例外）
- 覆盖 `TimetableDao.observeAllWithSemester` JOIN 查询和排序
- 覆盖批量操作（batchDelete、batchMove）
- 覆盖 `ColorPalette` 循环逻辑
- 提供端到端集成测试验证完整业务流

**Non-Goals:**
- 不做 UI/Espresso 测试
- 不引入 Mockito/Robolectric 等新依赖

## Decisions

### D1: 仪器测试统一使用 inMemory + allowMainThreadQueries

沿用 `CourseRepositoryTest` 的模式：`Room.inMemoryDatabaseBuilder().allowMainThreadQueries().build()`。Repository 的异步操作通过 `Thread.sleep` 等待完成。

**替代方案**: 使用 CountDownLatch 等待异步完成。放弃——对已有 ExecutorService 封装侵入性大，sleep 方式已被现有测试验证可行。

### D2: LiveData 测试使用 observeForever + CountDownLatch

`observeWeekSessions` 返回 LiveData，仪器测试中需要 `observeForever` 收集值。用 `CountDownLatch(1)` 等待首次发射后在 `onChanged` 中 countDown。

### D3: 单元测试 vs 仪器测试分层

| 测试类型 | 覆盖范围 | 位置 |
|----------|---------|------|
| 单元测试 (test/) | ColorPalette、DisplaySession | 无需 Context |
| 仪器测试 (androidTest/) | Repository、DAO、集成 | 需要真实 Room + Context |

### D4: 测试辅助方法复用

在仪器测试中提取公共的 entity 构建方法（`newSemester`、`newTimetable`、`newCourse`、`newSession`），避免重复代码。

## Risks / Trade-offs

- [Thread.sleep 可能导致 flaky 测试] → 使用 200ms 等待（现有测试已验证足够），CI 环境可调大
- [LiveData 测试需要主线程 Looper] → AndroidJUnit4 已提供主线程，用 InstrumentationRegistry.runOnMainSync 注册 observer
- [inMemory DB 不触发 Migration] → 迁移测试不在本次范围，用 latest schema 直接建库
