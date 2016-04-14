package com.best.android.cerocr;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;


public class CaptureActivity extends Activity {
    CapturePreview surfaceView;
    RelativeLayout tvCenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_capture);
        surfaceView = (CapturePreview)findViewById(R.id.activity_capture_surfaceView);
        tvCenter = (RelativeLayout)findViewById(R.id.activity_capture_tvCenter);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

}
