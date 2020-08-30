package com.plx.android.app;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.plx.android.wrouter.facade.annotation.Route;
import com.plx.android.wrouter.launcher.WRouter;

@Route(paths = "/main/hello")
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static Activity activity;
    Button btnInit;
    Button btnDebug;
    Button btnClose;
    Button btnTestJump;
    Button btnTestMoreJump;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnInit = findViewById(R.id.buttonPanel);
        btnDebug = findViewById(R.id.buttonPanel2);
        btnClose = findViewById(R.id.buttonPanel3);
        btnTestJump = findViewById(R.id.buttonPanel4);
        btnTestMoreJump = findViewById(R.id.buttonPanel5);
        btnInit.setOnClickListener(this);
        btnDebug.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        btnTestJump.setOnClickListener(this);
        btnTestMoreJump.setOnClickListener(this);

        activity = this;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.buttonPanel:
                WRouter.init(getApplication());
                break;
            case R.id.buttonPanel2:
                WRouter.openLog();
                break;
            case R.id.buttonPanel3:
                WRouter.getInstance().destroy();
                break;
            case R.id.buttonPanel4:
                WRouter.getInstance().build("/share/2").withString("name", "JHH").navigation();
                break;
            case R.id.buttonPanel5:
                WRouter.getInstance().build("/share/3333").navigation();
                break;
        }
    }

    public static Activity getThis(){
        return activity;
    }
}
