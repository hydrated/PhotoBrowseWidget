package com.awesome.hydra.photobrowsewidget.service;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.awesome.hydra.photobrowsewidget.provider.MyAppWidgetProvider;
import com.awesome.hydra.photobrowsewidget.storage.PhotoList;
import com.awesome.hydra.photobrowsewidget.util.GetPictureUtil;

import java.util.ArrayList;
import java.util.List;

import bolts.Continuation;
import bolts.Task;

public class PhotoService {

    private static final PhotoService INSTANCE = new PhotoService();

    public static PhotoService getInstance() {
        return INSTANCE;
    }

    public void queryPhotoUri(final Context context) {
        GetPictureUtil.getAllGalleryList(context.getApplicationContext()).continueWith(new Continuation<List<String>, Object>() {

            @Override
            public Object then(Task<List<String>> task) {
                PhotoList.photoGalleries = task.getResult();
                updateWidgets(context.getApplicationContext());
                return null;
            }
        });
    }

    private void updateWidgets(Context context) {
        Intent intent = new Intent(context, MyAppWidgetProvider.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // Use an array and EXTRA_APPWIDGET_IDS instead of AppWidgetManager.EXTRA_APPWIDGET_ID,
        // since it seems the onUpdate() is only fired on that:
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, MyAppWidgetProvider.class));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
            widgetManager.notifyAppWidgetViewDataChanged(ids, android.R.id.list);

        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }
}
