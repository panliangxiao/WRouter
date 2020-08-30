package com.plx.android.app;

import android.app.Application;
import android.content.Context;

import com.plx.android.wrouter.launcher.WRouter;

/**
 * Created by plx on 19/4/27.
 */

public class AppApplication extends Application {
    public static Context mAppContext;

    @Override
    public void onCreate() {
        super.onCreate();

        mAppContext = this.getApplicationContext();
//        WRouter.openDebug();
//        WRouter.openLog();
//        WRouter.init(this);
    }
}
