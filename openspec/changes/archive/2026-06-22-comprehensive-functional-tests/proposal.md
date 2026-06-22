## Why

当前测试覆盖仅限于工具类（WeekUtils、PeriodUtils、解析器）和一个 CourseRepository 的基础 CRUD 测试。核心业务逻辑——课表切换联动、学期唯一活跃约束、课程按课表过滤、批量操作、例外处理——完全没有测试覆盖。近期多次因课表切换逻辑和主线程数据库查询引入 bug，缺乏自动化测试保障。

## What Changes

- 新增 `TimetableRepository` 仪器测试：switchTo 联动学期、全局唯一活跃、创建/删除
- 新增 `SemesterRepository` 仪器测试：创建/删除/切换、活跃学期唯一性
- 扩展 `CourseRepository` 仪器测试：observeWeekSessions 过滤、batchDelete、batchMove、exception 联动
- 新增 `TimetableDao` 仪器测试：observeAllWithSemester JOIN、clearAllActive、observeActive
- 新增 `ColorPalette` 单元测试：defaultTag 循环、colorRes 边界
- 新增端到端集成测试：创建学期→创建课表→添加课程→切换课表→验证首页过滤结果

## Non-goals

- 不新增 UI 自动化测试（Espresso UI 流程不在本次范围）
- 不修改生产代码（除非测试发现 bug）
- 不新增测试框架（使用现有 JUnit4 + AndroidJUnit4 + Room inMemory）

## Capabilities

### New Capabilities
- `test-suite`: 全面功能测试套件，覆盖 Repository、DAO、核心业务逻辑层

### Modified Capabilities
_(无生产代码需求变更)_

## Impact

- 新增 `app/src/test/java/.../ui/common/ColorPaletteTest.java`
- 新增 `app/src/androidTest/java/.../data/repository/TimetableRepositoryTest.java`
- 新增 `app/src/androidTest/java/.../data/repository/SemesterRepositoryTest.java`
- 新增 `app/src/androidTest/java/.../data/repository/CourseRepositoryFilterTest.java`
- 新增 `app/src/androidTest/java/.../data/local/dao/TimetableDaoTest.java`
- 新增 `app/src/androidTest/java/.../IntegrationTest.java`
- 扩展 `CourseRepositoryTest.java`（补充 batch/exception 测试）
