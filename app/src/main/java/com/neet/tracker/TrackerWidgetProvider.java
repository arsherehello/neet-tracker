package com.neet.tracker;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.Locale;

public class TrackerWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAll(context, appWidgetManager, appWidgetIds);
    }

    public static void updateAll(Context context, AppWidgetManager mgr, int[] appWidgetIds) {
        DataStore store = new DataStore(context);
        for (int id : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_layout);

            double today = store.todayTotalHours();
            views.setTextViewText(R.id.widget_today_hours,
                    String.format(Locale.US, "%.1fh logged today", today));

            int physicsPct = (int) Math.round(store.subjectProgress("physics") * 100);
            int chemistryPct = (int) Math.round(store.subjectProgress("chemistry") * 100);
            int biologyPct = (int) Math.round(store.subjectProgress("biology") * 100);

            views.setTextViewText(R.id.widget_physics, "Physics " + physicsPct + "%");
            views.setProgressBar(R.id.widget_physics_bar, 100, physicsPct, false);

            views.setTextViewText(R.id.widget_chemistry, "Chemistry " + chemistryPct + "%");
            views.setProgressBar(R.id.widget_chemistry_bar, 100, chemistryPct, false);

            views.setTextViewText(R.id.widget_biology, "Biology " + biologyPct + "%");
            views.setProgressBar(R.id.widget_biology_bar, 100, biologyPct, false);

            Intent launch = new Intent(context, MainActivity.class);
            PendingIntent pi = PendingIntent.getActivity(context, 0, launch,
                    PendingIntent.FLAG_UPDATE_CURRENT | (android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));
            views.setOnClickPendingIntent(R.id.widget_root, pi);

            mgr.updateAppWidget(id, views);
        }
    }
}
