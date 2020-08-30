package com.plx.android.wrouter.core;

import android.content.Context;
import android.net.Uri;

import com.plx.android.wrouter.facade.Postcard;
import com.plx.android.wrouter.facade.model.RouteMeta;
import com.plx.android.wrouter.facade.template.IInterceptorGroup;
import com.plx.android.wrouter.facade.template.IProvider;
import com.plx.android.wrouter.facade.template.IProviderGroup;
import com.plx.android.wrouter.facade.template.IRouteGroup;
import com.plx.android.wrouter.facade.template.IRouteRoot;
import com.plx.android.wrouter.launcher.WRouter;
import com.plx.android.wrouter.utils.MapUtils;
import com.plx.android.wrouter.utils.TextUtils;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

import static com.plx.android.wrouter.launcher.WRouter.logger;
import static com.plx.android.wrouter.utils.Consts.TAG;

/**
 * Created by plx on 19/4/21.
 */

public class LogisticsCenter {
    private static Context mContext;
    static ThreadPoolExecutor executor;
    private static boolean registerByPlugin;

    /**
     * wrouter-auto-register plugin will generate code inside this method
     * call this method to register all Routers, Interceptors and Providers
     */
    private static void loadRouterMap() {
        registerByPlugin = false;
        //auto generate register code by gradle plugin: wrouter-auto-register
        // looks like below:
        // registerRouteRoot(new WRouter__Root__modulejava());
        // registerRouteRoot(new WRouter__Root__modulekotlin());
    }

    /**
     * register by object
     * Sacrificing a bit of efficiency to solve
     * the problem that the main dex file size is too large
     * @param obj class name
     */
    private static void register(Object obj) {
            try {
                if (obj instanceof IRouteRoot) {
                    registerRouteRoot((IRouteRoot) obj);
                } else if (obj instanceof IProviderGroup) {
                    registerProvider((IProviderGroup) obj);
                } else if (obj instanceof IInterceptorGroup) {
                    registerInterceptor((IInterceptorGroup) obj);
                } else {
                    logger.info(TAG, "register failed, class name: " + obj.toString()
                            + " should implements one of IRouteRoot/IProviderGroup/IInterceptorGroup.");
                }
            } catch (Exception e) {
                logger.error(TAG,"register class error:" + (obj == null ? "": obj.toString()));
            }
    }

    /**
     * method for wrouter-auto-register plugin to register Routers
     * @param routeRoot IRouteRoot implementation class in the package: com.plx.android.wrouter.core.routers
     */
    private static void registerRouteRoot(IRouteRoot routeRoot) {
        markRegisteredByPlugin();
        if (routeRoot != null) {
            routeRoot.loadInto(RouteTables.sGroupsIndex);
        }
    }

    /**
     * method for wrouter-auto-register plugin to register Providers
     * @param providerGroup IProviderGroup implementation class in the package: com.plx.android.wrouter.core.routers
     */
    private static void registerProvider(IProviderGroup providerGroup) {
        markRegisteredByPlugin();
        if (providerGroup != null) {
            providerGroup.loadInto(RouteTables.sProvidersIndex);
        }
    }

    /**
     * method for wrouter-auto-register plugin to register Interceptors
     * @param interceptorGroup IInterceptorGroup implementation class in the package: com.plx.android.wrouter.core.routers
     */
    private static void registerInterceptor(IInterceptorGroup interceptorGroup) {
        markRegisteredByPlugin();
        if (interceptorGroup != null) {
            interceptorGroup.loadInto(RouteTables.sInterceptorsIndex);
        }
    }


    /**
     * mark already registered by wrouter-auto-register plugin
     */
    private static void markRegisteredByPlugin() {
        if (!registerByPlugin) {
            registerByPlugin = true;
        }
    }

    /**
     * LogisticsCenter init, load all metas in memory. Demand initialization
     */
    public synchronized static void init(Context context, ThreadPoolExecutor tpe) throws RuntimeException {
        mContext = context;
        executor = tpe;

        try {
            long startInit = System.currentTimeMillis();
            //billy.qi modified at 2017-12-06
            //load by plugin first
            loadRouterMap();
            if (registerByPlugin) {
                logger.info(TAG, "Load router map by wrouter-auto-register plugin.");
            }
            logger.info(TAG, "Load root element finished, cost " + (System.currentTimeMillis() - startInit) + " ms.");

            if (RouteTables.sGroupsIndex.size() == 0) {
                logger.error(TAG, "No mapping files were found, check your configuration please!");
            }

            if (WRouter.debuggable()) {
                logger.debug(TAG, String.format(Locale.getDefault(), "LogisticsCenter has already been loaded, GroupIndex[%d], ProviderIndex[%d]", RouteTables.sGroupsIndex.size()/*, RouteTables.sInterceptorsIndex.size()*/, RouteTables.sProvidersIndex.size()));
            }
        } catch (Exception e) {
            throw new RuntimeException(TAG + "WRouter init logistics center exception! [" + e.getMessage() + "]");
        }
    }

    /**
     * Build postcard by serviceName
     *
     * @param serviceName interfaceName
     * @return postcard
     */
    public static Postcard buildProvider(String serviceName) {
        RouteMeta meta = RouteTables.sProvidersIndex.get(serviceName);

        if (null == meta) {
            return null;
        } else {
            return new Postcard(meta.getPath(), meta.getGroup());
        }
    }


    /**
     * Completion the postcard by route metas
     *
     * @param postcard Incomplete postcard, should complete by this method.
     */
    public synchronized static void completion(Postcard postcard) {
        if (null == postcard) {
            throw new RuntimeException(TAG + "No postcard!");
        }

        RouteMeta routeMeta = RouteTables.sRoutes.get(postcard.getPath());
        if (null == routeMeta) {    // Maybe its does't exist, or didn't load.
            Class<? extends IRouteGroup> groupMeta = RouteTables.sGroupsIndex.get(postcard.getGroup());  // Load route meta.
            if (null == groupMeta) {
                throw new RuntimeException(TAG + "There is no route match the path [" + postcard.getPath() + "], in group [" + postcard.getGroup() + "]");
            } else {
                // Load route and cache it into memory, then delete from metas.
                try {
                    if (WRouter.debuggable()) {
                        logger.debug(TAG, String.format(Locale.getDefault(), "The group [%s] starts loading, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }

                    IRouteGroup iGroupInstance = groupMeta.getConstructor().newInstance();
                    iGroupInstance.loadInto(RouteTables.sRoutes);
                    RouteTables.sGroupsIndex.remove(postcard.getGroup());

                    if (WRouter.debuggable()) {
                        logger.debug(TAG, String.format(Locale.getDefault(), "The group [%s] has already been loaded, trigger by [%s]", postcard.getGroup(), postcard.getPath()));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(TAG + "Fatal exception when loading group meta. [" + e.getMessage() + "]");
                }

                completion(postcard);   // Reload
            }
        } else {
            postcard.setDestination(routeMeta.getDestination());
            postcard.setType(routeMeta.getType());
            postcard.setPriority(routeMeta.getPriority());
            postcard.setExtra(routeMeta.getExtra());

            Uri rawUri = postcard.getUri();
            if (null != rawUri) {   // Try to set params into bundle.
//                Map<String, String> resultMap = TextUtils.splitQueryParameters(rawUri);
                Map<String, Integer> paramsType = routeMeta.getParamsType();

                if (MapUtils.isNotEmpty(paramsType)) {
                    // Set value by its type, just for params which annotation by @Param
//                    for (Map.Entry<String, Integer> params : paramsType.entrySet()) {
//                        setValue(postcard,
//                                params.getValue(),
//                                params.getKey(),
//                                resultMap.get(params.getKey()));
//                    }

                    // Save params name which need auto inject.
                    postcard.getExtras().putStringArray(WRouter.AUTO_INJECT, paramsType.keySet().toArray(new String[]{}));
                }

                // Save raw uri
                postcard.withString(WRouter.RAW_URI, rawUri.toString());
            }

            switch (routeMeta.getType()) {
                case PROVIDER:  // if the route is provider, should find its instance
                    // Its provider, so it must implement IProvider
                    Class<? extends IProvider> providerMeta = (Class<? extends IProvider>) routeMeta.getDestination();
                    IProvider instance = RouteTables.sProviders.get(providerMeta);
                    if (null == instance) { // There's no instance of this provider
                        IProvider provider;
                        try {
                            provider = providerMeta.getConstructor().newInstance();
                            provider.init(mContext);
                            RouteTables.sProviders.put(providerMeta, provider);
                            instance = provider;
                        } catch (Exception e) {
                            throw new RuntimeException("Init provider failed! " + e.getMessage());
                        }
                    }
                    postcard.setProvider(instance);
                    postcard.greenChannel();    // Provider should skip all of sInterceptors
                    break;
                case FRAGMENT:
                    postcard.greenChannel();    // Fragment needn't sInterceptors
                default:
                    break;
            }
        }
    }


    /**
     * Suspend bussiness, clear cache.
     */
    public static void suspend() {
        RouteTables.clear();
    }
}
