package com.kyselov.iradio.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;

import androidx.annotation.Nullable;

import com.kyselov.iradio.ApplicationLoader;
import com.kyselov.iradio.R;

public class ExternalActionActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        ApplicationLoader.postInitApplication();
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setTheme(R.style.AppTheme);
        getWindow().setBackgroundDrawableResource(R.drawable.transparent);
        super.onCreate(savedInstanceState);

    }

    @Override
    protected void onPause() {
        super.onPause();
        ApplicationLoader.externalInterfacePaused = true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        ApplicationLoader.externalInterfacePaused = false;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
