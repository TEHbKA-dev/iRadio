package com.kyselov.iradio.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.kyselov.iradio.AndroidUtilities;
import com.kyselov.iradio.ApplicationLoader;

public class AppStartReceiver extends BroadcastReceiver {

    public void onReceive(Context context, Intent intent) {
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            AndroidUtilities.runOnUIThread(ApplicationLoader::startPushService);
        }
    }
}
