## 1. 单元测试

- [x] 1.1 `ColorPaletteTest`：defaultTag 循环（0..SIZE-1..0）、colorRes 边界值
- [x] 1.2 `DisplaySessionTest`：isCancelled 对 TYPE_CANCEL 返回 true、TYPE_MOVED 返回 false

## 2. DAO 仪器测试

- [x] 2.1 `TimetableDaoTest`：observeAllWithSemester JOIN 返回 semesterName；活跃学期排前
- [x] 2.2 `TimetableDaoTest`：clearAllActive 清除全部 isActive；setActive 只设一个
- [x] 2.3 `TimetableDaoTest`：observeActive 返回正确的活跃课表（每学期最多一个）

## 3. Repository 仪器测试

- [x] 3.1 `TimetableRepositoryTest`：switchTo 联动学期（semester isActive 跟随）
- [x] 3.2 `TimetableRepositoryTest`：switchTo 全局唯一活跃（跨学期切换清除旧课表 isActive）
- [x] 3.3 `TimetableRepositoryTest`：create 创建课表并关联正确 semesterId
- [x] 3.4 `SemesterRepositoryTest`：create 不自动激活（创建后 isActive = 0）
- [x] 3.5 `SemesterRepositoryTest`：switchTo 设目标学期为唯一活跃

## 4. CourseRepository 扩展测试

- [x] 4.1 `CourseRepositoryFilterTest`：observeWeekSessions 按课表过滤（A 课表的课程不出现在 B 课表结果中）
- [x] 4.2 `CourseRepositoryFilterTest`：observeWeekSessions 周次过滤（weekPattern 不匹配的 session 不出现）
- [x] 4.3 `CourseRepositoryFilterTest`：CANCEL 例外排除、MOVED 例外改天
- [x] 4.4 `CourseRepositoryBatchTest`：batchDelete 删除多个课程 + 级联 session/exception
- [x] 4.5 `CourseRepositoryBatchTest`：batchMove 转移课程到目标课表

## 5. 集成测试

- [x] 5.1 `IntegrationTest`：创建学期→创建课表→添加课程→switchTo→验证 observeWeekSessions 过滤结果

## 6. 构建与运行

- [x] 6.1 编译通过：`./gradlew assembleDebug` + `./gradlew assembleDebugAndroidTest`
- [x] 6.2 运行仪器测试：`./gradlew connectedCheck`（或 adb instrument）
