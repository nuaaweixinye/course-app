package com.courseshedule.widget;

import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.courseshedule.App;
import com.courseshedule.R;
import com.courseshedule.data.local.AppDatabase;
import com.courseshedule.data.local.entity.SemesterEntity;
import com.courseshedule.data.local.entity.SessionExceptionEntity;
import com.courseshedule.data.model.DisplaySession;
import com.courseshedule.data.model.PeriodTime;
import com.courseshedule.data.model.PeriodUtils;
import com.courseshedule.data.model.SessionWithCourse;
import com.courseshedule.data.model.WeekUtils;
import com.courseshedule.ui.common.ColorPalette;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Supplies today's remaining courses to the widget. Queries synchronously
 * (RemoteViewsFactory runs on a worker thread), applies current-week +
 * exceptions identically to the grid, and keeps only sessions whose end time
 * is later than now.
 */
public class TodayWidgetFactory implements RemoteViewsService.RemoteViewsFactory {

    private final Context context;
    private final AppDatabase db;
    private List<DisplaySession> items = new ArrayList<>();

    public TodayWidgetFactory(Context context) {
        this.context = context;
        this.db = ((App) context.getApplicationContext()).getDatabase();
    }

    @Override
    public void onCreate() {}

    @Override
    public void onDestroy() {}

    @Override
    public void onDataSetChanged() {
        items = queryTodayRemaining();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        DisplaySession s = items.get(position);
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_item);
        views.setTextViewText(R.id.tvWidgetName, s.courseName);
        views.setTextViewText(R.id.tvWidgetMeta,
                context.getString(R.string.session_primary, "", s.startPeriod, s.endPeriod).trim());
        views.setInt(R.id.vBar, "setBackgroundColor",
                context.getResources().getColor(ColorPalette.colorRes(s.colorTag)));
        // Fill-in template so tapping a row opens the app at MainActivity.
        Intent fillIn = new Intent();
        views.setOnClickFillInIntent(R.id.tvWidgetName, fillIn);
        return views;
    }

    @Override
    public RemoteViews getLoadingView() { return null; }

    @Override
    public int getViewTypeCount() { return 1; }

    @Override
    public long getItemId(int position) { return position; }

    @Override
    public boolean hasStableIds() { return false; }

    private List<DisplaySession> queryTodayRemaining() {
        SemesterEntity cfg = new com.courseshedule.data.repository.SemesterRepository(db)
                .getSeedingDefault();
        int weekNo = WeekUtils.currentWeek(cfg.startDate, cfg.totalWeeks,
                System.currentTimeMillis());
        List<PeriodTime> periods = PeriodUtils.parse(cfg.periodTimesJson);

        Calendar c = Calendar.getInstance();
        int todayMonFirst = (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) ? 7
                : c.get(Calendar.DAY_OF_WEEK) - 1;
        int nowMinutes = c.get(Calendar.HOUR_OF_DAY) * 60 + c.get(Calendar.MINUTE);

        List<SessionWithCourse> all = db.courseSessionDao().listAllWithCourse();
        List<SessionExceptionEntity> exceptions = db.sessionExceptionDao().listAll();
        Map<Long, SessionExceptionEntity> exMap = new HashMap<>();
        for (SessionExceptionEntity ex : exceptions) {
            if (ex.weekNo == weekNo) exMap.put(ex.sessionId, ex);
        }

        List<DisplaySession> out = new ArrayList<>();
        for (SessionWithCourse s : all) {
            if (!WeekUtils.matchesWeek(s.weekPattern, weekNo)) continue;
            SessionExceptionEntity ex = exMap.get(s.sessionId);
            if (DisplaySession.isCancelled(ex)) continue;
            int day = s.dayOfWeek;
            if (ex != null && ex.type == SessionExceptionEntity.TYPE_MOVED && ex.moveToDayOfWeek != null) {
                day = ex.moveToDayOfWeek;
            }
            if (day != todayMonFirst) continue;
            // End time must be later than now.
            int endMin = endMinutes(periods, s.endPeriod);
            if (endMin < nowMinutes) continue;
            out.add(new DisplaySession(s.sessionId, s.courseId, s.courseName, s.teacher,
                    s.location, s.colorTag, day, s.startPeriod, s.endPeriod, s.weekPattern));
        }
        return out;
    }

    private int endMinutes(List<PeriodTime> periods, int period) {
        for (PeriodTime p : periods) if (p.index == period) return p.endMinutes;
        return 1440;
    }
}
