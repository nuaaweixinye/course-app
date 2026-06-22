package com.courseshedule.data.model;

import com.courseshedule.data.local.entity.SessionExceptionEntity;

import org.junit.Test;
import static org.junit.Assert.*;

public class DisplaySessionTest {

    @Test
    public void isCancelledReturnsFalseForNull() {
        assertFalse(DisplaySession.isCancelled(null));
    }

    @Test
    public void isCancelledReturnsTrueForCancelType() {
        SessionExceptionEntity ex = new SessionExceptionEntity();
        ex.type = SessionExceptionEntity.TYPE_CANCEL;
        assertTrue(DisplaySession.isCancelled(ex));
    }

    @Test
    public void isCancelledReturnsFalseForMovedType() {
        SessionExceptionEntity ex = new SessionExceptionEntity();
        ex.type = SessionExceptionEntity.TYPE_MOVED;
        assertFalse(DisplaySession.isCancelled(ex));
    }
}
