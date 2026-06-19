package com.courseshedule.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.courseshedule.R;
import com.courseshedule.ui.main.MainActivity;

/**
 * 4×2 home-screen widget showing today's remaining courses. Delegates content
 * to TodayWidgetService (RemoteViewsFactory). A tap anywhere opens the app.
 */
public class TodayWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_today);

            Intent serviceIntent = new Intent(context, TodayWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);
            views.setRemoteAdapter(R.id.listWidget, serviceIntent);
            views.setEmptyView(R.id.listWidget, R.id.tvWidgetEmpty);

            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(context, 0, openIntent,
                    PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
            views.setPendingIntentTemplate(R.id.listWidget, pi);
            // Header click also opens the app.
            views.setOnClickPendingIntent(R.id.tvWidgetHeader, pi);

            appWidgetManager.updateAppWidget(id, views);
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    /** Ask all instances to refresh their data. Call after schedule mutations. */
    public static void refresh(Context context) {
        AppWidgetManager mgr = AppWidgetManager.getInstance(context);
        ComponentName provider = new ComponentName(context, TodayWidgetProvider.class);
        int[] ids = mgr.getAppWidgetIds(provider);
        if (ids.length > 0) {
            mgr.notifyAppWidgetViewDataChanged(ids, R.id.listWidget);
        }
    }
}
