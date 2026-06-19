package com.courseshedule.widget;

import android.content.Intent;
import android.widget.RemoteViewsService;

/** Service backing the widget's ListView; returns a RemoteViewsFactory. */
public class TodayWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsService.RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new TodayWidgetFactory(getApplicationContext());
    }
}
