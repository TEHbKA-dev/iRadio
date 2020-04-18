package com.kyselov.iradio.service;

import android.app.IntentService;
import android.content.Intent;

import com.kyselov.iradio.ui.MainActivity;

public class BringAppForegroundService extends IntentService {

    public BringAppForegroundService() {
        super("BringAppForegroundService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Intent intent2 = new Intent(this, MainActivity.class);
        intent2.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent2.setAction(Intent.ACTION_MAIN);
        startActivity(intent2);
    }
}
