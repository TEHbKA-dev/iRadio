package com.kyselov.iradio.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.kyselov.iradio.ApplicationLoader;

public class NotificationsService extends Service {

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();
        sendBroadcast(new Intent("com.kyselov.iradio.start"));
    }
}
