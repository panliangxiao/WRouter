package com.plx.android.wrouter.facade.service;


import com.plx.android.wrouter.facade.template.IProvider;

/**
 * Service for autowired.
 *
 */
public interface AutowiredService extends IProvider {

    /**
     * Autowired core.
     * @param instance the instance who need autowired.
     */
    void autowire(Object instance);
}
