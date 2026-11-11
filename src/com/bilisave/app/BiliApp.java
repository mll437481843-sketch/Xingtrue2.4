package com.bilisave.app;

import android.app.Application;
import android.os.Build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Local-only crash log so a failure can be diagnosed without any network reporting. */
public class BiliApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        final Thread.UncaughtExceptionHandler prev = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                pw.println(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(new Date()));
                pw.println("thread=" + t.getName() + " android=" + Build.VERSION.RELEASE
                        + " device=" + Build.MANUFACTURER + " " + Build.MODEL);
                e.printStackTrace(pw);
                pw.flush();
                File out = new File("/sdcard/Download/BiliSave-last-crash.txt");
                FileOutputStream fos = new FileOutputStream(out, false);
                fos.write(sw.toString().getBytes("UTF-8"));
                fos.close();
            } catch (Throwable ignored) {
            }
            if (prev != null) prev.uncaughtException(t, e);
        });
    }
}

