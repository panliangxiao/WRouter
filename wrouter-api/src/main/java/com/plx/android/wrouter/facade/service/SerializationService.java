package com.plx.android.wrouter.facade.service;


import com.plx.android.wrouter.facade.template.IProvider;

import java.lang.reflect.Type;

/**
 * Used for parse json string.
 *
 */
public interface SerializationService extends IProvider {

    /**
     * Parse json to object
     *
     * USE @parseObject PLEASE
     *
     * @param input json string
     * @param clazz object type
     * @return instance of object
     */
    @Deprecated
    <T> T json2Object(String input, Class<T> clazz);

    /**
     * Object to json
     *
     * @param instance obj
     * @return json string
     */
    String object2Json(Object instance);

    /**
     * Parse json to object
     *
     * @param input json string
     * @param clazz object type
     * @return instance of object
     */
    <T> T parseObject(String input, Type clazz);
}
