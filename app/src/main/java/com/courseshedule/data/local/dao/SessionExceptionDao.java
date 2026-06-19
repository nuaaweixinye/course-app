package com.courseshedule.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.courseshedule.data.local.entity.SessionExceptionEntity;

import java.util.List;

@Dao
public interface SessionExceptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(SessionExceptionEntity exception);

    @Query("DELETE FROM session_exceptions WHERE id = :id")
    void deleteById(long id);

    @Query("SELECT * FROM session_exceptions WHERE sessionId = :sessionId")
    List<SessionExceptionEntity> listBySession(long sessionId);

    @Query("SELECT * FROM session_exceptions WHERE sessionId = :sessionId AND weekNo = :weekNo")
    SessionExceptionEntity findBySessionAndWeek(long sessionId, int weekNo);

    @Query("SELECT * FROM session_exceptions")
    List<SessionExceptionEntity> listAll();

    @Query("SELECT * FROM session_exceptions")
    LiveData<List<SessionExceptionEntity>> observeAll();
    @Query("DELETE FROM session_exceptions WHERE sessionId IN (:sessionIds)")
    void deleteBySessionIds(java.util.List<Long> sessionIds);

    @Query("SELECT * FROM session_exceptions WHERE sessionId IN (:sessionIds)")
    List<SessionExceptionEntity> listBySessionIds(java.util.List<Long> sessionIds);
}
