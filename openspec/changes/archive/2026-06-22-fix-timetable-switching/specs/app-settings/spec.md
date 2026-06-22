## MODIFIED Requirements

### Requirement: Semester management in settings
The system SHALL let the user manage all semesters from within the Settings page. The user SHALL NOT be able to independently switch the active semester; semester switching SHALL only occur when switching the active timetable.

#### Scenario: View semesters list
- **WHEN** the user opens Settings and scrolls to the "学期" section
- **THEN** all semesters are listed with the active one marked

#### Scenario: Create semester from settings
- **WHEN** the user taps "新建学期" in the settings page
- **THEN** a dialog prompts for a name and creates the semester with default values

#### Scenario: Edit semester details
- **WHEN** the user taps a semester row
- **THEN** a dialog shows options: 编辑（名称、开学日期、总周数）/ 删除
- **AND** no "设为当前学期" option is shown

## ADDED Requirements

### Requirement: Timetable creation requires existing semester
The system SHALL prevent timetable creation when no semester exists. The user SHALL be prompted to create a semester first.

#### Scenario: No semester exists
- **WHEN** the user attempts to create a timetable and no semester exists
- **THEN** the system shows a message prompting the user to create a semester first
