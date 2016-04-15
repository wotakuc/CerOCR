package com.best.android.cerocr;

import android.content.Context;
import android.os.Environment;

import java.io.File;

/**
 * Created by bl05498 on 2016/4/14.
 */
public class FileUtil {
    public static String getCacheDir(Context context) {
        StringBuilder cachePath = new StringBuilder();
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            File file = context.getApplicationContext().getExternalFilesDir(null);
            if (file != null) {
                cachePath.append(file.getAbsolutePath());
            } else {
                cachePath.append(Environment.getExternalStorageDirectory().getPath()).append("/Android/data/")
                        .append(context.getApplicationContext().getPackageName())
                        .append("/files");
            }
        } else {
            cachePath.append(context.getApplicationContext().getFilesDir().getAbsolutePath());
        }
        return cachePath.toString();
    }
}
