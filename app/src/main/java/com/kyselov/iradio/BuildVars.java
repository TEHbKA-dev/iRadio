package com.kyselov.iradio;

import android.content.Context;
import android.content.SharedPreferences;

public class BuildVars {

    public static boolean DEBUG_VERSION;
    public static boolean LOGS_ENABLED = true;
    public static boolean CHECK_UPDATES = false;
    public static String TAG = "iRadio";
    public static int BUILD_VERSION;
    public static String BUILD_VERSION_STRING;
    public static boolean BUILD_FOR_TV;

    static {
        BUILD_VERSION = BuildConfig.VERSION_CODE;
        BUILD_VERSION_STRING = BuildConfig.VERSION_NAME;

        if (ApplicationLoader.applicationContext != null) {
            final SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = sharedPreferences.getBoolean("logsEnabled", BUILD_VERSION_STRING.contains(".beta"));
        }

        DEBUG_VERSION = BUILD_VERSION_STRING.contains(".beta");
        BUILD_FOR_TV = BUILD_VERSION_STRING.contains(".tv");
    }
}
