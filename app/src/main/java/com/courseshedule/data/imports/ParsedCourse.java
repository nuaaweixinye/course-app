package com.courseshedule.data.imports;

/** A parsed course waiting for preview/confirm before persistence. */
public class ParsedCourse {
    public String name;
    public String teacher;
    public final java.util.List<ParsedSession> sessions = new java.util.ArrayList<>();
}
