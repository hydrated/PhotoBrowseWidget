package com.awesome.hydra.photobrowsewidget.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.awesome.hydra.photobrowsewidget.R;
import com.awesome.hydra.photobrowsewidget.provider.MyAppWidgetProvider;
import com.awesome.hydra.photobrowsewidget.util.GetPictureUtil;
import com.awesome.hydra.photobrowsewidget.view.WidgetItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.AppWidgetTarget;

import bolts.Continuation;
import bolts.Task;

public class WidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StackRemoteViewsFactory(this.getApplicationContext(), intent);
    }
}

class StackRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {
    private final Context mContext;
    private int mAppWidgetId;

    public StackRemoteViewsFactory(Context context, Intent intent) {
        mContext = context;
        mAppWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID);
    }

    public void onCreate() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void onDestroy() {
        // In onDestroy() you should tear down anything that was setup for your data source,
        // eg. cursors, connections, etc.
    }

    public int getCount() {
        return MyAppWidgetProvider.photoGalleries.size();
    }

    public RemoteViews getViewAt(int position) {
        Log.d("hydrated", "position" + position);

        RemoteViews rv = new RemoteViews(mContext.getPackageName(), R.layout.widget_item);
        Uri uri = Uri.fromFile(new File(MyAppWidgetProvider.photoGalleries.get(position)));

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(mContext.getContentResolver(), uri);
            rv.setBitmap(R.id.widget_item, "setImageBitmap", bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }


        Bundle extras = new Bundle();
        extras.putInt("Some String", position);
        Intent fillInIntent = new Intent();
        fillInIntent.putExtras(extras);
        rv.setOnClickFillInIntent(R.id.widget_item, fillInIntent);

        return rv;
    }

    public RemoteViews getLoadingView() {
        return null;
    }

    public int getViewTypeCount() {
        return 1;
    }

    public long getItemId(int position) {
        return position;
    }

    public boolean hasStableIds() {
        return true;
    }

    public void onDataSetChanged() {

    }
}