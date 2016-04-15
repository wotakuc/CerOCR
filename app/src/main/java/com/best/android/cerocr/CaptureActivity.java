package com.best.android.cerocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class CaptureActivity extends Activity {
    private static final String tag = "CaptureActivity";
    CaptureSurfaceView captureSurfaceView;
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

        captureSurfaceView = (CaptureSurfaceView)findViewById(R.id.activity_capture_surfaceView);
        tvCenter = (RelativeLayout)findViewById(R.id.activity_capture_tvCenter);
        testImageView = (ImageView)findViewById(R.id.testImageView);

        btnPic = findViewById(R.id.activity_capture_btnPic);

        btnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePic();
            }
        });
    }

    private void takePic(){
        progressDialog.setMessage("识别中...");
        progressDialog.show();
        btnPic.setEnabled(false);
        captureSurfaceView.takePic(picCallBack);
    }


    Camera.PictureCallback picCallBack = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            CropTask task = new CropTask(data
                    ,captureSurfaceView.getWidth(),captureSurfaceView.getHeight()
                    ,tvCenter.getWidth(),tvCenter.getHeight()
                    ,tvCenter.getLeft(),tvCenter.getTop());
            task.execute();
        }
    };

    class CropTask extends AsyncTask<Object,String,Bitmap>{
        static final int DIFFNUM = 100;
        byte[] data;

        int screenWidth;
        int screenHeight;

        int centerWidth;
        int centerHeight;
        int centerLeft;
        int centerTop;

        public CropTask(byte[] data, int screenWidth, int screenHeight, int centerWidth, int centerHeight, int centerLeft, int centerTop) {
            this.data = data;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.centerWidth = centerWidth;
            this.centerHeight = centerHeight;
            this.centerLeft = centerLeft;
            this.centerTop = centerTop;
        }

        @Override
        protected Bitmap doInBackground(Object... params) {
            Log.d(tag,"center:" + centerWidth + "," + centerHeight);

            //创建拍照图片
            BitmapFactory.Options opt = new BitmapFactory.Options();
            opt.inPreferredConfig = Bitmap.Config.RGB_565;
            Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length,opt);
            Log.d(tag,"bmp width:" + bmp.getWidth() + "  height:" + bmp.getHeight());

            //先计算条码相对center在屏幕显示位置
            //对于856*540的身份证  code左上位置在270,420  右下780，495  width 510  height 75
            double codeLeft = 270 * centerWidth / 856;
            double codeTop = 420D * centerHeight /540;
            double codeWidth = 510D * centerWidth / 856;
            double codeHeight = 75D * centerHeight/ 540;
            Log.d(tag,"code:" + codeLeft + "," + codeTop + "," + codeWidth + "," + codeHeight);

            //计算条码在实际屏幕上的位置
            double sCodeLeft = codeLeft + centerLeft;
            double sCodeTop = codeTop + centerTop;
            double sCodeWidth = codeWidth;
            double sCodeHeight = codeHeight;
            Log.d(tag,"scode:" + sCodeLeft + "," + sCodeTop + "," + sCodeWidth + "," + sCodeHeight);

            //计算条码映射到图片中的位置
            int pCodeLeft = (int)(sCodeLeft * bmp.getWidth() / screenWidth);
            int pCodeTop = (int)(sCodeTop * bmp.getHeight() / screenHeight);
            int pCodeWidth = (int)(sCodeWidth * bmp.getWidth() / screenWidth);
            int pCodeHeight = (int)(sCodeHeight * bmp.getHeight() / screenHeight);
            Log.d(tag,"pcode:" + pCodeLeft + "," + pCodeTop + "," + pCodeWidth + "," + pCodeHeight);

            //裁剪出code图片
            Bitmap codeBmp = Bitmap.createBitmap(bmp,pCodeLeft,pCodeTop,pCodeWidth,pCodeHeight);
            Log.d(tag,"codeBmp width:" + codeBmp.getWidth() + "  height:" + codeBmp.getHeight());

            //灰度化
            int width = codeBmp.getWidth();         //获取位图的宽
            int height = codeBmp.getHeight();       //获取位图的高
            int []pixels = new int[width * height]; //通过位图的大小创建像素点数组
            codeBmp.getPixels(pixels, 0, width, 0, 0, width, height);
            int alpha = 0xFF << 24;
            for(int i = 0; i < height; i++)  {
                for(int j = 0; j < width; j++) {
                    int grey = pixels[width * i + j];

                    int red = ((grey  & 0x00FF0000 ) >> 16);
                    int green = ((grey & 0x0000FF00) >> 8);
                    int blue = (grey & 0x000000FF);

                    grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);

                    //去除部分噪点
                    if(grey>DIFFNUM)
                        grey = 255;

                    grey = alpha | (grey << 16) | (grey << 8) | grey;

                    pixels[width * i + j] = grey;
                }
            }
            Bitmap greyBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            greyBmp.setPixels(pixels, 0, width, 0, 0, width, height);

            bmp.recycle();
            bmp = null;
            codeBmp.recycle();;
            codeBmp = null;

            return greyBmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if(bitmap == null){
                Toast.makeText(CaptureActivity.this,"识别异常",Toast.LENGTH_SHORT).show();
            }
            else{
                testImageView.setImageBitmap(bitmap);
            }

            //继续下一次任务
            captureSurfaceView.getCamera().startPreview();
            progressDialog.dismiss();
            btnPic.setEnabled(true);
        }
    }

    private Bitmap cropCodePic(byte[] data){
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
        int pCodeLeft = (int)(sCodeLeft * bmp.getWidth() / captureSurfaceView.getWidth());
        int pCodeTop = (int)(sCodeTop * bmp.getHeight() / captureSurfaceView.getHeight());
        int pCodeWidth = (int)(sCodeWidth * bmp.getWidth() / captureSurfaceView.getWidth());
        int pCodeHeight = (int)(sCodeHeight * bmp.getHeight() / captureSurfaceView.getHeight());
        Log.d(tag,"pcode:" + pCodeLeft + "," + pCodeTop + "," + pCodeWidth + "," + pCodeHeight);

        Bitmap codeBmp = Bitmap.createBitmap(bmp,pCodeLeft,pCodeTop,pCodeWidth,pCodeHeight);
        bmp.recycle();
        bmp = null;
        Log.d(tag,"codeBmp width:" + codeBmp.getWidth() + "  height:" + codeBmp.getHeight());

        return codeBmp;
    }


}
