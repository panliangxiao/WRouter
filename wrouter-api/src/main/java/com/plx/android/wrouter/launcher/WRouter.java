package com.plx.android.wrouter.launcher;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import com.plx.android.wrouter.facade.Postcard;
import com.plx.android.wrouter.facade.callback.NavigationCallback;
import com.plx.android.wrouter.facade.template.ILogger;
import com.plx.android.wrouter.utils.Consts;

/**
 * Created by plx on 19/4/21.
 */

public class WRouter {
    // Key of raw uri
    public static final String RAW_URI = "NTeRQWvye18AkPd6G";
    public static final String AUTO_INJECT = "wmHzgD4lOj5o4241";

    private volatile static WRouter instance = null;
    private volatile static boolean hasInit = false;

    public static ILogger logger;

    private WRouter() {
    }

    /**
     * Init, it must be call before used router.
     */
    public static void init(Application application) {
        if (!hasInit) {
            logger = _WRouter.logger;
            _WRouter.logger.info(Consts.TAG, "WRouter init start.");
            hasInit = _WRouter.init(application);

            if (hasInit) {
                _WRouter.afterInit();
            }

            _WRouter.logger.info(Consts.TAG, "WRouter init over.");
        }
    }

    /**
     * Get instance of router. A
     * All feature U use, will be starts here.
     */
    public static WRouter getInstance() {
        if (!hasInit) {
            throw new IllegalStateException("WRouter::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (WRouter.class) {
                    if (instance == null) {
                        instance = new WRouter();
                    }
                }
            }
            return instance;
        }
    }

    public static synchronized void openDebug() {
        _WRouter.openDebug();
    }

    public static boolean debuggable() {
        return _WRouter.debuggable();
    }

    public static synchronized void openLog() {
        _WRouter.openLog();
    }

    public static synchronized void printStackTrace() {
        _WRouter.printStackTrace();
    }

    public synchronized void destroy() {
        _WRouter.destroy();
        hasInit = false;
    }

    /**
     * Inject params and services.
     */
    public void inject(Object thiz) {
        _WRouter.inject(thiz);
    }

    /**
     * Build the roadmap, draw a postcard.
     *
     * @param path Where you go.
     */
    public Postcard build(String path) {
        return _WRouter.getInstance().build(path);
    }


    /**
     * Build the roadmap, draw a postcard.
     *
     * @param url the path
     */
    public Postcard build(Uri url) {
        return _WRouter.getInstance().build(url);
    }

    /**
     * Launch the navigation by type
     *
     * @param service interface of service
     * @param <T>     return type
     * @return instance of service
     */
    public  <T> T navigation(Class<? extends T> service) {
        return _WRouter.getInstance().navigation(service);
    }

    /**
     * Launch the navigation.
     *
     * @param mContext    .
     * @param postcard    .
     * @param requestCode Set for startActivityForResult
     * @param callback    cb
     */
    public Object navigation(Context mContext, Postcard postcard, int requestCode, NavigationCallback callback) {
        return _WRouter.getInstance().navigation(mContext, postcard, requestCode, callback);
    }
}
