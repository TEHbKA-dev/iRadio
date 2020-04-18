package com.kyselov.iradio;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.PowerManager;

import com.google.firebase.database.FirebaseDatabase;
import com.kyselov.iradio.receiver.ScreenReceiver;
import com.kyselov.iradio.service.NotificationsService;

public class ApplicationLoader extends Application {

    public static volatile Context applicationContext;
    public static volatile Handler applicationHandler;


    private static volatile boolean applicationInited = false;
    public static volatile boolean isScreenOn = false;
    public static volatile boolean externalInterfacePaused = true;

    public static void postInitApplication() {
        if (applicationInited) {
            return;
        }

        applicationInited = true;

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("app initied");
        }

        try {
            final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
            filter.addAction(Intent.ACTION_SCREEN_OFF);
            final BroadcastReceiver mReceiver = new ScreenReceiver();
            applicationContext.registerReceiver(mReceiver, filter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
            if (pm != null)
                isScreenOn = pm.isInteractive();
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("screen state = " + isScreenOn);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }

    }

    public ApplicationLoader() {
        super();
    }

    @Override
    public void onCreate() {

        try {
            applicationContext = getApplicationContext();
        } catch (Throwable ignore) {

        }

        super.onCreate();

        if (applicationContext == null) {
            applicationContext = getApplicationContext();
        }

        applicationHandler = new Handler(applicationContext.getMainLooper());

        AndroidUtilities.runOnUIThread(ApplicationLoader::startPushService);
    }

    public static void startPushService() {

        try {
            applicationContext.startService(new Intent(applicationContext, NotificationsService.class));
        } catch (Throwable ignore) {

        }
    }
}
