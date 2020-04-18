package com.kyselov.iradio.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kyselov.iradio.ApplicationLoader;
import com.kyselov.iradio.BuildVars;
import com.kyselov.iradio.FileLog;
import com.kyselov.iradio.EventCenter;

public class ScreenReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("screen off");
                }
                ApplicationLoader.isScreenOn = false;
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d("screen on");
                }
                ApplicationLoader.isScreenOn = true;
            }
        }
        EventCenter.get().postEventName(EventCenter.screenStateChanged);
    }
}
