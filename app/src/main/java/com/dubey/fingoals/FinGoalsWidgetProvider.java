package com.dubey.fingoals;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FinGoalsWidgetProvider extends AppWidgetProvider {
    private static final ExecutorService io = Executors.newSingleThreadExecutor();

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        FinanceCore.Data cached = FinanceCore.parse(FinanceCore.loadCache(context));
        updateAll(context, appWidgetManager, appWidgetIds, cached);

        io.execute(() -> {
            try {
                String json = FinanceCore.fetchJson();
                FinanceCore.saveCache(context, json);
                FinanceCore.Data live = FinanceCore.parse(json);
                int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, FinGoalsWidgetProvider.class));
                updateAll(context, appWidgetManager, ids, live);
            } catch (Exception ignored) {}
        });
    }

    static void updateAll(Context context, AppWidgetManager manager, int[] ids, FinanceCore.Data data) {
        if (ids == null) return;
        FinanceCore.Summary s = FinanceCore.summarize(data);
        for (int id : ids) updateOne(context, manager, id, s);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id, FinanceCore.Summary s) {
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.fin_goals_widget);
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);
        views.setTextViewText(R.id.widget_title, "FinGoals");
        views.setTextViewText(R.id.widget_line1, "Invested " + FinanceCore.inr(s.totalInvestments));
        views.setTextViewText(R.id.widget_line2, "UPI " + FinanceCore.inr(s.sachinUpi + s.ashaUpi) + " · Cards " + FinanceCore.inr(s.unbilledCards));
        manager.updateAppWidget(id, views);
    }
}
