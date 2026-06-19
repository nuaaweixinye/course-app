package com.courseshedule.data.imports;

/** Raised when an importer cannot parse its source. */
public class ImportException extends Exception {
    public ImportException(String message) { super(message); }
    public ImportException(String message, Throwable cause) { super(message, cause); }
}
