package com.best.android.cerocr;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;

import jp.wasabeef.blurry.Blurry;

public class CaptureActivity extends Activity {
    CapturePreview surfaceView;
    RelativeLayout tvTop;
    RelativeLayout tvRight;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_capture);
        surfaceView = (CapturePreview)findViewById(R.id.activity_capture_surfaceView);
        tvTop = (RelativeLayout)findViewById(R.id.activity_capture_tvTop);
        tvRight = (RelativeLayout)findViewById(R.id.activity_capture_tvRight);
    }

    @Override
    protected void onResume() {
        super.onResume();

//        blurry();
    }

    public void blurry(){
//        Blurry.with(CaptureActivity.this).radius(25).sampling(8).onto(tvTop);
//        Blurry.with(CaptureActivity.this).radius(25).sampling(8).onto(tvRight);
    }

}
