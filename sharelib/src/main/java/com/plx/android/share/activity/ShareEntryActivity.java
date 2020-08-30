package com.plx.android.share.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.TextView;

import com.plx.android.wrouter.facade.annotation.Autowired;
import com.plx.android.wrouter.facade.annotation.Route;
import com.plx.android.share.R;
import com.plx.android.wrouter.launcher.WRouter;

/**
 * Created by plx on 19/4/21.
 */

@Route(paths = "/share/2")
public class ShareEntryActivity extends Activity{

    @Autowired
    public static String name;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WRouter.getInstance().inject(this);
        setContentView(R.layout.activity_text2);

        TextView textView = findViewById(R.id.name);
        textView.setText(name);
    }
}
