package com.plx.android.wrouter.launcher;

import android.app.Activity;
import android.app.Application;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.plx.android.wrouter.core.LogisticsCenter;
import com.plx.android.wrouter.facade.Postcard;
import com.plx.android.wrouter.facade.callback.InterceptorCallback;
import com.plx.android.wrouter.facade.callback.NavigationCallback;
import com.plx.android.wrouter.facade.service.AutowiredService;
import com.plx.android.wrouter.facade.service.DegradeService;
import com.plx.android.wrouter.facade.service.InterceptorService;
import com.plx.android.wrouter.facade.service.PathReplaceService;
import com.plx.android.wrouter.facade.template.ILogger;
import com.plx.android.wrouter.thread.DefaultPoolExecutor;
import com.plx.android.wrouter.utils.Consts;
import com.plx.android.wrouter.utils.DefaultLogger;
import com.plx.android.wrouter.utils.TextUtils;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Created by plx on 19/4/21.
 */

public class _WRouter {
    static ILogger logger = new DefaultLogger(Consts.TAG);
    private volatile static boolean monitorMode = false;
    private volatile static boolean debuggable = false;
    private volatile static boolean autoInject = false;
    private volatile static _WRouter instance = null;
    private volatile static boolean hasInit = false;
    private volatile static ThreadPoolExecutor executor = DefaultPoolExecutor.getInstance();
    private static Handler mHandler;
    private static Context mContext;

    private static InterceptorService interceptorService;

    private _WRouter() {
    }

    protected static synchronized boolean init(Application application) {
        mContext = application;
        LogisticsCenter.init(mContext, executor);
        logger.info(Consts.TAG, "WRouter init success!");
        hasInit = true;
        mHandler = new Handler(Looper.getMainLooper());

        return true;
    }

    /**
     * Destroy wrouter, it can be used only in debug mode.
     */
    static synchronized void destroy() {
        if (debuggable()) {
            hasInit = false;
            LogisticsCenter.suspend();
            logger.info(Consts.TAG, "WRouter destroy success!");
        } else {
            logger.error(Consts.TAG, "Destroy can be used in debug mode only!");
        }
    }

    static synchronized void openDebug() {
        debuggable = true;
        logger.info(Consts.TAG, "WRouter openDebug");
    }

    static synchronized void openLog() {
        logger.showLog(true);
        logger.info(Consts.TAG, "WRouter openLog");
    }

    static synchronized void printStackTrace() {
        logger.showStackTrace(true);
        logger.info(Consts.TAG, "WRouter printStackTrace");
    }

    protected static _WRouter getInstance() {
        if (!hasInit) {
            throw new IllegalStateException("WRouterCore::Init::Invoke init(context) first!");
        } else {
            if (instance == null) {
                synchronized (_WRouter.class) {
                    if (instance == null) {
                        instance = new _WRouter();
                    }
                }
            }
            return instance;
        }
    }



    static boolean debuggable() {
        return debuggable;
    }


    static void afterInit() {
        // Trigger interceptor init, use byName.
        interceptorService = (InterceptorService) WRouter.getInstance().build(Consts.WROUTER_INTERCEPTOR_SERVICE).navigation();
    }

    static void inject(Object thiz) {
        AutowiredService autowiredService = ((AutowiredService) WRouter.getInstance().build(Consts.WROUTER_AUTOWIRED_SERVICE).navigation());
        if (null != autowiredService) {
            autowiredService.autowire(thiz);
        }
    }

    /**
     * Build postcard by path and default group
     */
    protected Postcard build(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException(Consts.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService pService = WRouter.getInstance().navigation(PathReplaceService.class);
            if (null != pService) {
                path = pService.forString(path);
            }
            return build(path, extractGroup(path));
        }
    }

    /**
     * Build postcard by uri
     */
    protected Postcard build(Uri uri) {
        if (null == uri || TextUtils.isEmpty(uri.toString())) {
            throw new RuntimeException(Consts.TAG + "Parameter invalid!");
        } else {
            PathReplaceService pService = WRouter.getInstance().navigation(PathReplaceService.class);
            if (null != pService) {
                uri = pService.forUri(uri);
            }
            return new Postcard(uri.getPath(), extractGroup(uri.getPath()), uri, null);
        }
    }

    /**
     * Build postcard by path and group
     */
    protected Postcard build(String path, String group) {
        if (TextUtils.isEmpty(path) || TextUtils.isEmpty(group)) {
            throw new RuntimeException(Consts.TAG + "Parameter is invalid!");
        } else {
            PathReplaceService pService = WRouter.getInstance().navigation(PathReplaceService.class);
            if (null != pService) {
                path = pService.forString(path);
            }
            return new Postcard(path, group);
        }
    }

    /**
     * Extract the default group from path.
     */
    private String extractGroup(String path) {
        if (TextUtils.isEmpty(path) || !path.startsWith("/")) {
            throw new RuntimeException(Consts.TAG + "Extract the default group failed, the path must be start with '/' and contain more than 2 '/'!");
        }

        try {
            String defaultGroup = path.substring(1, path.indexOf("/", 1));
            if (TextUtils.isEmpty(defaultGroup)) {
                throw new RuntimeException(Consts.TAG + "Extract the default group failed! There's nothing between 2 '/'!");
            } else {
                return defaultGroup;
            }
        } catch (Exception e) {
            logger.warning(Consts.TAG, "Failed to extract default group! " + e.getMessage());
            return null;
        }
    }

    protected <T> T navigation(Class<? extends T> service) {
        try {
            Postcard postcard = LogisticsCenter.buildProvider(service.getName());

            // Compatible 1.0.5 compiler sdk.
            // Earlier versions did not use the fully qualified name to get the service
            if (null == postcard) {
                // No service, or this service in old version.
                postcard = LogisticsCenter.buildProvider(service.getSimpleName());
            }

            if (null == postcard) {
                return null;
            }

            LogisticsCenter.completion(postcard);
            return (T) postcard.getProvider();
        } catch (Exception ex) {
            logger.warning(Consts.TAG, ex.getMessage());
            return null;
        }
    }

    /**
     * Use router navigation.
     *
     * @param context     Activity or null.
     * @param postcard    Route metas
     * @param requestCode RequestCode
     * @param callback    cb
     */
    protected Object navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        try {
            LogisticsCenter.completion(postcard);
        } catch (Exception ex) {
            logger.warning(Consts.TAG, ex.getMessage());

            if (debuggable()) {
                // Show friendly tips for user.
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(mContext, "There's no route matched!\n" +
                                " Path = [" + postcard.getPath() + "]\n" +
                                " Group = [" + postcard.getGroup() + "]", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (null != callback) {
                callback.onLost(postcard);
            } else {    // No callback for this invoke, then we use the global degrade service.
                DegradeService degradeService = WRouter.getInstance().navigation(DegradeService.class);
                if (null != degradeService) {
                    degradeService.onLost(context, postcard);
                }
            }

            return null;
        }

        if (null != callback) {
            callback.onFound(postcard);
        }

        if (!postcard.isGreenChannel()) {   // It must be run in async thread, maybe interceptor cost too mush time made ANR.
            interceptorService.doInterceptions(postcard, new InterceptorCallback() {
                /**
                 * Continue process
                 *
                 * @param postcard route meta
                 */
                @Override
                public void onContinue(Postcard postcard) {
                    _navigation(context, postcard, requestCode, callback);
                }

                /**
                 * Interrupt process, pipeline will be destory when this method called.
                 *
                 * @param exception Reson of interrupt.
                 */
                @Override
                public void onInterrupt(Throwable exception) {
                    if (null != callback) {
                        callback.onInterrupt(postcard);
                    }

                    logger.info(Consts.TAG, "Navigation failed, termination by interceptor : " + exception.getMessage());
                }
            });
        } else {
            return _navigation(context, postcard, requestCode, callback);
        }

        return null;
    }

    private Object _navigation(final Context context, final Postcard postcard, final int requestCode, final NavigationCallback callback) {
        final Context currentContext = null == context ? mContext : context;

        switch (postcard.getType()) {
            case ACTIVITY:
                // Build intent
                final Intent intent = new Intent(currentContext, postcard.getDestination());
                intent.putExtras(postcard.getExtras());

                // Set flags.
                int flags = postcard.getFlags();
                if (-1 != flags) {
                    intent.setFlags(flags);
                } else if (!(currentContext instanceof Activity)) {    // Non activity, need less one flag.
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                }

                // Set Actions
                String action = postcard.getAction();
                if (!TextUtils.isEmpty(action)) {
                    intent.setAction(action);
                }

                // Navigation in main looper.
                runInMainThread(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(requestCode, currentContext, intent, postcard, callback);
                    }
                });

                break;
            case PROVIDER:
                return postcard.getProvider();
            case BOARDCAST:
            case CONTENT_PROVIDER:
            case FRAGMENT:
                Class fragmentMeta = postcard.getDestination();
                try {
                    Object instance = fragmentMeta.getConstructor().newInstance();
                    if (instance instanceof Fragment) {
                        ((Fragment) instance).setArguments(postcard.getExtras());
                    } else if (instance instanceof android.support.v4.app.Fragment) {
                        ((android.support.v4.app.Fragment) instance).setArguments(postcard.getExtras());
                    }

                    return instance;
                } catch (Exception ex) {
                    logger.error(Consts.TAG, "Fetch fragment instance error, " + TextUtils.formatStackTrace(ex.getStackTrace()));
                }
            case METHOD:
            case SERVICE:
            default:
                return null;
        }

        return null;
    }

    /**
     * Be sure execute in main thread.
     *
     * @param runnable code
     */
    private void runInMainThread(Runnable runnable) {
        if (Looper.getMainLooper().getThread() != Thread.currentThread()) {
            mHandler.post(runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Start activity
     * @see ActivityCompat
     */
    private void startActivity(int requestCode, Context currentContext, Intent intent, Postcard postcard, NavigationCallback callback) {
        if (requestCode >= 0) {  // Need start for result
            if (currentContext instanceof Activity) {
                ActivityCompat.startActivityForResult((Activity) currentContext, intent, requestCode, postcard.getOptionsBundle());
            } else {
                logger.warning(Consts.TAG, "Must use [navigation(activity, ...)] to support [startActivityForResult]");
            }
        } else {
            ActivityCompat.startActivity(currentContext, intent, postcard.getOptionsBundle());
        }

        if ((-1 != postcard.getEnterAnim() && -1 != postcard.getExitAnim()) && currentContext instanceof Activity) {    // Old version.
            ((Activity) currentContext).overridePendingTransition(postcard.getEnterAnim(), postcard.getExitAnim());
        }

        if (null != callback) { // Navigation over.
            callback.onArrival(postcard);
        }
    }
}
