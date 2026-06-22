## 1. 数据层

- [x] 1.1 创建 `TimetableWithSemester` POJO（包含 timetable 字段 + `semesterName` 字段）
- [x] 1.2 `TimetableDao`：新增 `@Query` JOIN 查询 `observeAllWithSemester()` 返回 `LiveData<List<TimetableWithSemester>>`，活跃学期在前，其余按 startDate 降序
- [x] 1.3 `TimetableRepository`：新增 `observeAllWithSemester()` 方法透传 DAO 结果

## 2. 学期管理页精简

- [x] 2.1 `item_semester_row.xml`：移除 `btnManageTimetables` 按钮及相关布局属性
- [x] 2.2 `SemesterManageActivity`：移除 `openTimetableManage()` 方法、`OnTimetableClick` 接口、Adapter 中 `btnTt` 绑定

## 3. 课表管理页重构

- [x] 3.1 `item_timetable_row.xml`：新增 `tvSemesterName` TextView，显示所属学期名称
- [x] 3.2 `TimetableManageActivity`：移除 `EXTRA_SEMESTER_ID` 依赖，改为加载 `observeAllWithSemester()`
- [x] 3.3 `TimetableManageActivity.Adapter`：`onBindViewHolder` 中绑定 `semesterName` 到 `tvSemesterName`
- [x] 3.4 `TimetableManageActivity`：新建课表对话框改为自定义布局（学期 Spinner + 课表名 EditText），Spinner 数据源为全部学期，默认选中活跃学期
- [x] 3.5 `TimetableManageActivity`：导入逻辑 (`confirmImport`) 改为使用活跃学期 ID 而非 Extra

## 4. 设置页入口

- [x] 4.1 `SettingsActivity`："管理课表"按钮启动 `TimetableManageActivity` 时不传 `EXTRA_SEMESTER_ID`
- [x] 4.2 移除 `SettingsActivity` 中"管理课表"按钮可能存在的学期相关逻辑

## 5. 新增字符串资源

- [x] 5.1 `strings.xml`：新增新建课表对话框所需字符串（如 `hint_timetable_name`、`label_select_semester` 等，按需）

## 6. 构建与验证

- [x] 6.1 编译通过，无错误
- [x] 6.2 安装并测试：课表管理显示全部课表及学期名；新建课表可选学期；学期管理无课表入口；设置页直接打开课表管理
