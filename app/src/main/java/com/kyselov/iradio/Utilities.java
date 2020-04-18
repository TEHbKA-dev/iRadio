package com.kyselov.iradio;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.swiperefreshlayout.widget.CircularProgressDrawable;

import java.io.File;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.Locale;

public class Utilities {
    public static volatile DispatchQueue stageQueue = new DispatchQueue("stageQueue");
    public static volatile DispatchQueue globalQueue = new DispatchQueue("globalQueue");
    public static volatile Locale defaultLocale = Locale.getDefault();
    public static SecureRandom random = new SecureRandom();

    static {

        try {
            final File URANDOM_FILE = new File("/dev/urandom");
            final FileInputStream sUrandomIn = new FileInputStream(URANDOM_FILE);
            final byte[] buffer = new byte[1024];
            sUrandomIn.read(buffer);
            sUrandomIn.close();
            random.setSeed(buffer);
        } catch (Exception e) {
            FileLog.e(e);
        }

    }

    public static String format(final String format, final Object... args) {
        return String.format(defaultLocale, format, args);
    }
}
