package com.plx.android.wrouter.core;

import android.content.Context;
import android.util.LruCache;

import com.plx.android.wrouter.facade.annotation.Route;
import com.plx.android.wrouter.facade.service.AutowiredService;
import com.plx.android.wrouter.facade.template.ISyringe;

import java.util.ArrayList;
import java.util.List;

import static com.plx.android.wrouter.utils.Consts.SUFFIX_AUTOWIRED;
import static com.plx.android.wrouter.utils.Consts.WROUTER_AUTOWIRED_SERVICE;


/**
 * Autowired service impl.
 *
 */
@Route(paths = WROUTER_AUTOWIRED_SERVICE)
public class AutowiredServiceImpl implements AutowiredService {
    private LruCache<String, ISyringe> classCache;
    private List<String> blackList;

    @Override
    public void init(Context context) {
        classCache = new LruCache<>(66);
        blackList = new ArrayList<>();
    }

    @Override
    public void autowire(Object instance) {
        String className = instance.getClass().getName();
        try {
            if (!blackList.contains(className)) {
                ISyringe autowiredHelper = classCache.get(className);
                if (null == autowiredHelper) {  // No cache.
                    autowiredHelper = (ISyringe) Class.forName(instance.getClass().getName() + SUFFIX_AUTOWIRED).getConstructor().newInstance();
                }
                autowiredHelper.inject(instance);
                classCache.put(className, autowiredHelper);
            }
        } catch (Exception ex) {
            blackList.add(className);    // This instance need not autowired.
        }
    }
}
