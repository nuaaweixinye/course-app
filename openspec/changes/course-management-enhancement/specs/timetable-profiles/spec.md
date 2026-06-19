# timetable-profiles Specification

## Purpose
Let users create named profiles within a semester to show/hide subsets of courses on the grid. Profiles are non-destructive filters that can be toggled independently.

## ADDED Requirements

### Requirement: Create a profile
The system SHALL let the user create a named profile (e.g., "专业课", "公共课") within the active semester.

#### Scenario: Create profile assigns tag
- **WHEN** the user creates a profile named "专业课"
- **THEN** a profile entry is stored with the given name for the active semester

#### Scenario: Profile list persists
- **WHEN** the user switches away from the semester and back
- **THEN** previously created profiles for that semester are still available

### Requirement: Assign/unassign courses to a profile
The system SHALL let the user add or remove a course from one or more profiles via the course detail/edit screen.

#### Scenario: Assign course to profile
- **WHEN** the user edits course "高等数学" and checks the "专业课" profile checkbox
- **THEN** the course's profile tags include "专业课"

#### Scenario: Remove course from profile
- **WHEN** the user unchecks "专业课" on the course edit screen
- **THEN** the "专业课" tag is removed from the course

### Requirement: Toggle profile visibility on the grid
The system SHALL let the user toggle profiles on/off from the main screen using chips or a bottom sheet. Only courses assigned to at least one active profile SHALL appear on the grid. If no profiles are active, all courses SHALL appear.

#### Scenario: Profile filter hides courses
- **WHEN** the user has profiles "专业课" and "公共课" and togglesON only "专业课"
- **THEN** the grid shows only courses tagged with "专业课"
- **AND** courses tagged only with "公共课" are hidden

#### Scenario: No profiles active shows all
- **WHEN** all profile toggles are OFF
- **THEN** the grid shows all courses for the active semester (unfiltered)

#### Scenario: Course in multiple profiles
- **WHEN** a course is tagged with both "专业课" and "公共课"
- **AND** both profiles are ON
- **THEN** the course appears once on the grid (no duplicates)
