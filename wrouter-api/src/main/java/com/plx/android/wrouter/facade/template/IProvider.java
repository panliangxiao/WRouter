package com.plx.android.wrouter.facade.template;

import android.content.Context;

public interface IProvider {

    /**
     * Do your init work in this method, it well be call when processor has been load.
     *
     * @param context ctx
     */
    void init(Context context);
}
