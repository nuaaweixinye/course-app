package com.courseshedule.data.imports;

/**
 * Contract for any schedule source (file or future 教务系统). Implementations
 * parse their source into {@link ParsedCourse}s; the same preview/confirm flow
 * in ImportActivity handles the rest.
 */
public interface TimetableImporter {
    /**
     * Parse the source and return parsed courses.
     *
     * @throws ImportException on unrearsable input.
     */
    java.util.List<ParsedCourse> fetch() throws ImportException;
}
