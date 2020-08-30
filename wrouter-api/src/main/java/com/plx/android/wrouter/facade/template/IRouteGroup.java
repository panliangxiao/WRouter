package com.plx.android.wrouter.facade.template;

import com.plx.android.wrouter.facade.model.RouteMeta;

import java.util.Map;

public interface IRouteGroup {
    /**
     * Fill the atlas with routes in group.
     */
    void loadInto(Map<String, RouteMeta> atlas);
}