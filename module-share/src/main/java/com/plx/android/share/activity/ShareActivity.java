package com.plx.android.share.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.plx.android.wrouter.facade.annotation.Route;
import com.plx.android.share.R;

/**
 * Created by plx on 19/4/21.
 */

@Route(paths = {"/share/1", "/share2/1", "/share/3333"})
public class ShareActivity extends Activity{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_text1);
    }
}
