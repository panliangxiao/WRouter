package com.plx.android.wrouter.facade.service;

import android.content.Context;

import com.plx.android.wrouter.facade.Postcard;
import com.plx.android.wrouter.facade.template.IProvider;


/**
 * Provide degrade service for router, you can do something when route has lost.
 *
 */
public interface DegradeService extends IProvider {

    /**
     * Router has lost.
     *
     * @param postcard meta
     */
    void onLost(Context context, Postcard postcard);
}
