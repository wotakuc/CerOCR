package com.best.android.cerocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;


public class CaptureActivity extends Activity {
    private static final String tag = "CaptureActivity";
    CapturePreview surfaceView;
    RelativeLayout tvCenter;
    View btnPic;

    ImageView testImageView;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(R.layout.activity_capture);

        progressDialog = new ProgressDialog(this);

        surfaceView = (CapturePreview)findViewById(R.id.activity_capture_surfaceView);
        tvCenter = (RelativeLayout)findViewById(R.id.activity_capture_tvCenter);
        testImageView = (ImageView)findViewById(R.id.testImageView);

        btnPic = findViewById(R.id.activity_capture_btnPic);
        btnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.setMessage("识别中...");
                progressDialog.show();
                surfaceView.takeAndOcrPic(picCallBack);
            }
        });
    }

    Camera.PictureCallback picCallBack = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(tag,"center:" + tvCenter.getWidth() + "," + tvCenter.getHeight());
            Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
            Log.d(tag,"bmp width:" + bmp.getWidth() + "  height:" + bmp.getHeight());

            //先计算条码相对center在屏幕显示位置
            //对于856*540的身份证  code左上位置在270,420  右下780，495  width 510  height 75
            double codeLeft = 270 * tvCenter.getWidth() / 856;
            double codeTop = 420D * tvCenter.getHeight() /540;
            double codeWidth = 510D * tvCenter.getWidth() / 856;
            double codeHeight = 75D * tvCenter.getHeight() / 540;
            Log.d(tag,"code:" + codeLeft + "," + codeTop + "," + codeWidth + "," + codeHeight);

            //计算条码在实际屏幕上的位置
            double sCodeLeft = codeLeft + tvCenter.getLeft();
            double sCodeTop = codeTop + tvCenter.getTop();
            double sCodeWidth = codeWidth;
            double sCodeHeight = codeHeight;
            Log.d(tag,"scode:" + sCodeLeft + "," + sCodeTop + "," + sCodeWidth + "," + sCodeHeight);

            //计算条码映射到图片中的位置
            int pCodeLeft = (int)(sCodeLeft * bmp.getWidth() / surfaceView.getWidth());
            int pCodeTop = (int)(sCodeTop * bmp.getHeight() / surfaceView.getHeight());
            int pCodeWidth = (int)(sCodeWidth * bmp.getWidth() / surfaceView.getWidth());
            int pCodeHeight = (int)(sCodeHeight * bmp.getHeight() / surfaceView.getHeight());
            Log.d(tag,"pcode:" + pCodeLeft + "," + pCodeTop + "," + pCodeWidth + "," + pCodeHeight);

            Bitmap codeBmp = Bitmap.createBitmap(bmp,pCodeLeft,pCodeTop,pCodeWidth,pCodeHeight);
            Log.d(tag,"codeBmp width:" + codeBmp.getWidth() + "  height:" + codeBmp.getHeight());

            int offerset = surfaceView.getWidth() * surfaceView.getHeight();
            Log.d(tag,"dataLength:" + data.length);
            Log.d(tag,"surfaceView length:" + offerset);

            testImageView.setImageBitmap(codeBmp);

            surfaceView.getCamera().startPreview();
            progressDialog.dismiss();
        }
    };

    static class PicCropTask extends AsyncTask {


        @Override
        protected Object doInBackground(Object[] params) {
            byte[] data = (byte[]) params[0];

            return null;
        }
    }

}
