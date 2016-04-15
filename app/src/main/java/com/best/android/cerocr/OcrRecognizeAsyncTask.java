package com.best.android.cerocr;

import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.leptonica.android.ReadFile;
import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Created by bl05498 on 2016/4/15.
 */
public class OcrRecognizeAsyncTask extends AsyncTask<Void, Void, Boolean> {
    private CaptureActivity activity;
    private TessBaseAPI baseApi;
    private Bitmap bitmap;
    String textResult;
    long timeRequired;

    public OcrRecognizeAsyncTask(CaptureActivity activity,TessBaseAPI baseApi,Bitmap bitmap) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.bitmap = bitmap;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        long start = System.currentTimeMillis();
        try {
            baseApi.setImage(ReadFile.readBitmap(bitmap));
            textResult = baseApi.getUTF8Text();
            timeRequired = System.currentTimeMillis() - start;
            // Check for failure to recognize text
            if (textResult == null || textResult.equals("")) {
                return false;
            }
        } catch (Exception e) {
            Log.e("OcrRecognizeAsyncTask", "Caught RuntimeException in request to Tesseract.");
            e.printStackTrace();
            try {
                baseApi.clear();
            } catch (NullPointerException e1) {
                // Continue
            }
            return false;
        }
        timeRequired = System.currentTimeMillis() - start;
        return true;
    }

    @Override
    protected void onPostExecute(Boolean aBoolean) {
        super.onPostExecute(aBoolean);
        if(aBoolean){
            activity.setTextResult(textResult,timeRequired);
            activity.next();
        }else {
            Toast.makeText(activity,"OCR failed. Please try again.",Toast.LENGTH_SHORT).show();
            activity.next();
        }

        if (baseApi != null) {
            baseApi.clear();
        }
    }
}
