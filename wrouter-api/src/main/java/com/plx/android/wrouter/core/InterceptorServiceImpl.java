package com.plx.android.wrouter.core;

import android.content.Context;


import com.plx.android.wrouter.facade.Postcard;
import com.plx.android.wrouter.facade.annotation.Route;
import com.plx.android.wrouter.facade.callback.InterceptorCallback;
import com.plx.android.wrouter.facade.service.InterceptorService;
import com.plx.android.wrouter.facade.template.IInterceptor;
import com.plx.android.wrouter.thread.CancelableCountDownLatch;
import com.plx.android.wrouter.utils.MapUtils;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.plx.android.wrouter.launcher.WRouter.logger;
import static com.plx.android.wrouter.utils.Consts.TAG;
import static com.plx.android.wrouter.utils.Consts.WROUTER_INTERCEPTOR_SERVICE;


/**
 * All of interceptors
 *
 */
@Route(paths = WROUTER_INTERCEPTOR_SERVICE)
public class InterceptorServiceImpl implements InterceptorService {
    private static boolean interceptorHasInit;
    private static final Object interceptorInitLock = new Object();

    @Override
    public void doInterceptions(final Postcard postcard, final InterceptorCallback callback) {
        if (null != RouteTables.sInterceptors && RouteTables.sInterceptors.size() > 0) {

            checkInterceptorsInitStatus();

            if (!interceptorHasInit) {
                callback.onInterrupt(new RuntimeException("Interceptors initialization takes too much time."));
                return;
            }

            LogisticsCenter.executor.execute(new Runnable() {
                @Override
                public void run() {
                    CancelableCountDownLatch interceptorCounter = new CancelableCountDownLatch(RouteTables.sInterceptors.size());
                    try {
                        _excute(0, interceptorCounter, postcard);
                        interceptorCounter.await(postcard.getTimeout(), TimeUnit.SECONDS);
                        if (interceptorCounter.getCount() > 0) {    // Cancel the navigation this time, if it hasn't return anythings.
                            callback.onInterrupt(new RuntimeException("The interceptor processing timed out."));
                        } else if (null != postcard.getTag()) {    // Maybe some exception in the tag.
                            callback.onInterrupt(new RuntimeException(postcard.getTag().toString()));
                        } else {
                            callback.onContinue(postcard);
                        }
                    } catch (Exception e) {
                        callback.onInterrupt(e);
                    }
                }
            });
        } else {
            callback.onContinue(postcard);
        }
    }

    /**
     * Excute interceptor
     *
     * @param index    current interceptor index
     * @param counter  interceptor counter
     * @param postcard routeMeta
     */
    private static void _excute(final int index, final CancelableCountDownLatch counter, final Postcard postcard) {
        if (index < RouteTables.sInterceptors.size()) {
            IInterceptor iInterceptor = RouteTables.sInterceptors.get(index);
            iInterceptor.process(postcard, new InterceptorCallback() {
                @Override
                public void onContinue(Postcard postcard) {
                    // Last interceptor excute over with no exception.
                    counter.countDown();
                    _excute(index + 1, counter, postcard);  // When counter is down, it will be execute continue ,but index bigger than interceptors size, then U know.
                }

                @Override
                public void onInterrupt(Throwable exception) {
                    // Last interceptor excute over with fatal exception.

                    postcard.setTag(null == exception ? new RuntimeException("No message.") : exception.getMessage());    // save the exception message for backup.
                    counter.cancel();
                    // Be attention, maybe the thread in callback has been changed,
                    // then the catch block(L207) will be invalid.
                    // The worst is the thread changed to main thread, then the app will be crash, if you throw this exception!
//                    if (!Looper.getMainLooper().equals(Looper.myLooper())) {    // You shouldn't throw the exception if the thread is main thread.
//                        throw new HandlerException(exception.getMessage());
//                    }
                }
            });
        }
    }

    @Override
    public void init(final Context context) {
        LogisticsCenter.executor.execute(new Runnable() {
            @Override
            public void run() {
                if (MapUtils.isNotEmpty(RouteTables.sInterceptorsIndex)) {
                    for (Map.Entry<Integer, Class<? extends IInterceptor>> entry : RouteTables.sInterceptorsIndex.entrySet()) {
                        Class<? extends IInterceptor> interceptorClass = entry.getValue();
                        try {
                            IInterceptor iInterceptor = interceptorClass.getConstructor().newInstance();
                            iInterceptor.init(context);
                            RouteTables.sInterceptors.add(iInterceptor);
                        } catch (Exception ex) {
                            throw new RuntimeException(TAG + "WRouter init interceptor error! name = [" + interceptorClass.getName() + "], reason = [" + ex.getMessage() + "]");
                        }
                    }

                    interceptorHasInit = true;

                    logger.info(TAG, "WRouter interceptors init over.");

                    synchronized (interceptorInitLock) {
                        interceptorInitLock.notifyAll();
                    }
                }
            }
        });
    }

    private static void checkInterceptorsInitStatus() {
        synchronized (interceptorInitLock) {
            while (!interceptorHasInit) {
                try {
                    interceptorInitLock.wait(10 * 1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(TAG + "Interceptor init cost too much time error! reason = [" + e.getMessage() + "]");
                }
            }
        }
    }
}
