package com.example.uncaughtexception;

import android.app.Application;

/**
 * Created by Administrator on 2017/11/22.
 */

public class CrashApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CrashHandler crashHandler = CrashHandler.getInstance();
        crashHandler.init(this);
    }

}
