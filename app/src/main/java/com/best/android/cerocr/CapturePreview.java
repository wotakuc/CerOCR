package com.best.android.cerocr;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;

import java.io.IOException;
import java.util.List;

/**
 * Created by bl02512 on 2016/4/13.
 */
public class CapturePreview extends SurfaceView implements SurfaceHolder.Callback, Camera.AutoFocusCallback{
    private static final String tag = "CapturePreview";

    Camera mCamera;
    Camera.Size previewSize;
    SurfaceHolder mHolder;
    AutoFocusManager focusManager;

    static final int MIN_PREVIEW_PIXELS = 480 * 320;
    static final int MAX_PREVIEW_PIXELS = 1920 * 1080;

    public CapturePreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mHolder = holder;
        safeCameraOpen();
        focusManager = new AutoFocusManager(mCamera, true, this);
        focusManager.start();
    }

    public void onAutoFocus(boolean success, Camera camera) {
        Log.d(tag,"onAutoFocus  focusSuccess  " + success);
    }

    private boolean safeCameraOpen() {
        boolean qOpened = false;
        try {
            stopPreviewAndFreeCamera();
            mCamera = Camera.open();
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(tag, "failed to open Camera");
            e.printStackTrace();
        }
        return qOpened;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            previewSize = findBestSize(mCamera.getParameters().getSupportedPreviewSizes());
            requestLayout();

            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }

//            mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            focusManager.start();
        }
    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreviewAndFreeCamera();

        if(focusManager!=null){
            focusManager.stop();
            focusManager = null;
        }
    }

    private void stopPreviewAndFreeCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    Camera.Size findBestSize(List<Camera.Size> sizes) {
        if (sizes == null || sizes.isEmpty()) {
            return null;
        }
        int width = getWidth();
        int height = getHeight();
        Camera.Size bestSize = null;
        int mPixels = width * height;
        int diffSize = Integer.MAX_VALUE;
        for (Camera.Size size : sizes) {
            int supportPixels = size.width * size.height;
            if (supportPixels < MIN_PREVIEW_PIXELS || supportPixels > MAX_PREVIEW_PIXELS) {
                continue;
            }
            if (supportPixels == mPixels)
                return size;
            int diff = Math.abs(mPixels - supportPixels);
            if (diff < diffSize) {
                diffSize = diff;
                bestSize = size;
            }
        }
        return bestSize;
    }
}
