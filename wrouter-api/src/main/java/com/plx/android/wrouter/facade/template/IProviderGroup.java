package com.plx.android.wrouter.facade.template;

import com.plx.android.wrouter.facade.model.RouteMeta;

import java.util.Map;

public interface IProviderGroup {
    /**
     * Load providers map to input
     *
     * @param providers input
     */
    void loadInto(Map<String, RouteMeta> providers);
}