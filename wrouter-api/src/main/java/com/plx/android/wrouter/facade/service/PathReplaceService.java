package com.plx.android.wrouter.facade.service;

import android.net.Uri;

import com.plx.android.wrouter.facade.template.IProvider;


/**
 * Preprocess your path
 *
 */
public interface PathReplaceService extends IProvider {

    /**
     * For normal path.
     *
     * @param path raw path
     */
    String forString(String path);

    /**
     * For uri type.
     *
     * @param uri raw uri
     */
    Uri forUri(Uri uri);
}
