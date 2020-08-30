package com.plx.android.wrouter.core;

import com.plx.android.wrouter.base.UniqueKeyTreeMap;
import com.plx.android.wrouter.facade.model.RouteMeta;
import com.plx.android.wrouter.facade.template.IInterceptor;
import com.plx.android.wrouter.facade.template.IProvider;
import com.plx.android.wrouter.facade.template.IRouteGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by plx on 19/4/23.
 */

public class RouteTables {

    // Cache route and metas
    static Map<String, Class<? extends IRouteGroup>> sGroupsIndex = new HashMap<>();
    static Map<String, RouteMeta> sRoutes = new HashMap<>();

    // Cache provider
    static Map<Class, IProvider> sProviders = new HashMap<>();
    static Map<String, RouteMeta> sProvidersIndex = new HashMap<>();

    // Cache interceptor
    static Map<Integer, Class<? extends IInterceptor>> sInterceptorsIndex = new UniqueKeyTreeMap<>("More than one sInterceptors use same priority [%s]");
    static List<IInterceptor> sInterceptors = new ArrayList<>();


    static void clear() {
        sRoutes.clear();
        sGroupsIndex.clear();
        sProviders.clear();
        sProvidersIndex.clear();
        sInterceptors.clear();
        sInterceptorsIndex.clear();
    }

}
