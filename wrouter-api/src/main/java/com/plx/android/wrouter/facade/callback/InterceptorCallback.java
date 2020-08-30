package com.plx.android.wrouter.facade.callback;


import com.plx.android.wrouter.facade.Postcard;

/**
 * The callback of interceptor.
 *
 */
public interface InterceptorCallback {

    /**
     * Continue process
     *
     * @param postcard route meta
     */
    void onContinue(Postcard postcard);

    /**
     * Interrupt process, pipeline will be destory when this method called.
     *
     * @param exception Reson of interrupt.
     */
    void onInterrupt(Throwable exception);
}
