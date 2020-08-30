package com.plx.android.wrouter.facade.template;

import java.util.Map;

public interface IRouteRoot {

    /**
     * Load routes to input
     * @param routes input
     */
    void loadInto(Map<String, Class<? extends IRouteGroup>> routes);
}