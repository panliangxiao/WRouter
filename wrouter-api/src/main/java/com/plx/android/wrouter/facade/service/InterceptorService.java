package com.plx.android.wrouter.facade.service;


import com.plx.android.wrouter.facade.Postcard;
import com.plx.android.wrouter.facade.callback.InterceptorCallback;
import com.plx.android.wrouter.facade.template.IProvider;

/**
 * Interceptor service
 *
 * @author zhilong <a href="mailto:zhilong.lzl@alibaba-inc.com">Contact me.</a>
 * @version 1.0
 * @since 2017/2/23 下午2:06
 */
public interface InterceptorService extends IProvider {

    /**
     * Do interceptions
     */
    void doInterceptions(Postcard postcard, InterceptorCallback callback);
}
