package com.best.android.cerocr;

import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by bl05498 on 2016/4/14.
 */
public class ImageUtil {
    public static boolean saveBitmap(String path, Bitmap bitmap, int quality) {
        if (TextUtils.isEmpty(path) || bitmap == null)
            return false;
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
        if (f.exists())
            f.delete();
        try {
            FileOutputStream out = new FileOutputStream(f);
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            out.flush();
            out.close();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
