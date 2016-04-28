package com.best.android.cerocr;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.util.UUID;


public class CaptureActivity extends Activity {
    private static final String tag = "CaptureActivity";
    CaptureSurfaceView captureSurfaceView;
    RelativeLayout tvCenter;
    View btnPic;

    ImageView testImageView;
    TextView testResult;
    TextView testTime;

    ProgressDialog progressDialog;

    TessBaseAPI baseApi;

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
        testResult = (TextView)findViewById(R.id.testResult);
        testTime = (TextView)findViewById(R.id.testTime);

        btnPic = findViewById(R.id.activity_capture_btnPic);
        setButtonClickable(false);
        btnPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePic();
            }
        });
        initOcrEngine();
    }

    private void takePic(){
        progressDialog.setMessage("识别中...");
        progressDialog.setIndeterminate(true);
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
        static final int DIFFNUM = 120;
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
            opt.inPreferredConfig = Bitmap.Config.ARGB_8888;
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
//            int width = codeBmp.getWidth();         //获取位图的宽
//            int height = codeBmp.getHeight();       //获取位图的高
//            int []pixels = new int[width * height]; //通过位图的大小创建像素点数组
//            codeBmp.getPixels(pixels, 0, width, 0, 0, width, height);
//            int alpha = 0xFF << 24;
//            for(int i = 0; i < height; i++)  {
//                for(int j = 0; j < width; j++) {
//                    int grey = pixels[width * i + j];
//
//                    int red = ((grey  & 0x00FF0000 ) >> 16);
//                    int green = ((grey & 0x0000FF00) >> 8);
//                    int blue = (grey & 0x000000FF);
//
//                    grey = (int)((float) red * 0.3 + (float)green * 0.59 + (float)blue * 0.11);
//
////                    //去除部分噪点
////                    if(grey>DIFFNUM)
////                        grey = 255;
//
//                    grey = alpha | (grey << 16) | (grey << 8) | grey;
//
//                    pixels[width * i + j] = grey;
//                }
//            }
            //必须要ARGB_8888才能用于OCR识别
//            Bitmap greyBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//            greyBmp.setPixels(pixels, 0, width, 0, 0, width, height);

//            bmp.recycle();
//            bmp = null;
//            codeBmp.recycle();
//            codeBmp = null;

            //存下来图片来
            String path = FileUtil.getCacheDir(CaptureActivity.this)+ File.separator + "_" + UUID.randomUUID().toString() + ".jpg";
            ImageUtil.saveBitmap(path, codeBmp, 100);

            return codeBmp;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            if(bitmap == null){
                Toast.makeText(CaptureActivity.this,"识别异常",Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
            else{
                testImageView.setImageBitmap(bitmap);
                progressDialog.setMessage(" 正在识别中......");
                new OcrRecognizeAsyncTask(CaptureActivity.this,baseApi,bitmap).execute();
            }
        }
    }
    void next(){
        //继续下一次任务
        progressDialog.dismiss();
        captureSurfaceView.getCamera().startPreview();
        captureSurfaceView.getFocusManager().start();
        setButtonClickable(true);
    }

    void initOcrEngine(){
        btnPic.setEnabled(false);
        btnPic.setClickable(false);
        // Initialize the OCR engine
        File storageDirectory = getStorageDirectory();
        if (storageDirectory != null) {
            String languageCode = "eng";
            String languageName = "English";
            int orcEngineMode = TessBaseAPI.OEM_TESSERACT_ONLY;
            baseApi = new TessBaseAPI();
            new OcrInitAsyncTask(this,baseApi,progressDialog,languageCode,languageName,orcEngineMode).execute(storageDirectory.toString());
        }
    }
    void resumeOCR(){
        if (baseApi != null) {
            //todo 待检查
            baseApi.setVariable("load_system_dawg",TessBaseAPI.VAR_FALSE);
            baseApi.setVariable("load_freq_dawg",TessBaseAPI.VAR_FALSE);
            baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO_OSD);
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "");
            baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789X");
        }
        //可以开始识别了
        setButtonClickable(true);
    }

    void setTextResult(String text,long timeRequired){
        if(TextUtils.isEmpty(text)){
            Toast.makeText(this, "OCR failed. Please try again.", Toast.LENGTH_SHORT).show();
        }
        testResult.setText(text);
        testTime.setText("Time required: "+timeRequired + "ms");
    }

    void setButtonClickable(boolean clickable){
        btnPic.setEnabled(clickable);
        btnPic.setClickable(clickable);
    }


    /** Finds the proper location on the SD card where we can save files. */
    private File getStorageDirectory() {
        //Log.d(TAG, "getStorageDirectory(): API level is " + Integer.valueOf(android.os.Build.VERSION.SDK_INT));

        String state = null;
        try {
            state = Environment.getExternalStorageState();
        } catch (RuntimeException e) {
            Log.e(tag, "Is the SD card visible?", e);
            Toast.makeText(this,"Error,Required external storage (such as an SD card) is unavailable.",Toast.LENGTH_SHORT).show();
        }

        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {

            // We can read and write the media
            //    	if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) > 7) {
            // For Android 2.2 and above

            try {
                return getExternalFilesDir(Environment.MEDIA_MOUNTED);
            } catch (NullPointerException e) {
                // We get an error here if the SD card is visible, but full
                Log.e(tag, "External storage is unavailable");
                Toast.makeText(this,"Error,Required external storage (such as an SD card) is full or unavailable.",Toast.LENGTH_LONG).show();
            }

            //        } else {
            //          // For Android 2.1 and below, explicitly give the path as, for example,
            //          // "/mnt/sdcard/Android/data/edu.sfsu.cs.orange.ocr/files/"
            //          return new File(Environment.getExternalStorageDirectory().toString() + File.separator +
            //                  "Android" + File.separator + "data" + File.separator + getPackageName() +
            //                  File.separator + "files" + File.separator);
            //        }

        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // We can only read the media
            Log.e(tag, "External storage is read-only");
            Toast.makeText(this,"Error,Required external storage (such as an SD card) is unavailable for data storage.",Toast.LENGTH_LONG).show();
        } else {
            // Something else is wrong. It may be one of many other states, but all we need
            // to know is we can neither read nor write
            Log.e(tag, "External storage is unavailable");
            Toast.makeText(this,"Error,Required external storage (such as an SD card) is unavailable or corrupted.",Toast.LENGTH_LONG).show();
        }
        return null;
    }

    @Override
    protected void onDestroy() {
        if (baseApi != null) {
            baseApi.end();
        }
        super.onDestroy();
    }
}



