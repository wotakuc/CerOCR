package com.best.android.cerocr;

import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by bl05498 on 2016/4/15.
 */
public class OcrInitAsyncTask extends AsyncTask<String, String, Boolean> {
    static final String TAG = OcrInitAsyncTask.class.getSimpleName();
    CaptureActivity activity;
    TessBaseAPI baseApi;
    String languageCode;
    String languageName;
    int ocrEngineMode;
    ProgressDialog dialog;

    public OcrInitAsyncTask(CaptureActivity activity, TessBaseAPI baseApi,ProgressDialog dialog,String languageCode, String languageName, int ocrEngineMode) {
        this.activity = activity;
        this.baseApi = baseApi;
        this.languageCode = languageCode;
        this.languageName = languageName;
        this.ocrEngineMode = ocrEngineMode;
        this.dialog = dialog;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        dialog.setMessage("init OcrEngine");
        dialog.setIndeterminate(false);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setCancelable(false);
        dialog.show();
    }
    /**
     * In background thread, perform required setup, and request initialization of
     * the OCR engine.
     *
     * @param params
     *          [0] Pathname for the directory for storing language data files to the SD card
     */
    @Override
    protected Boolean doInBackground(String... params) {
        // The storage directory, minus the "tessdata" subdirectory
        String destinationDirBase = params[0];
        String destinationFilenameBase = languageCode + ".traineddata";
        File tessdataDir = new File(destinationDirBase + File.separator + "tessdata");
        if (!tessdataDir.exists() && !tessdataDir.mkdirs()) {
            Log.e(TAG, "Couldn't make directory " + tessdataDir);
            return false;
        }
        File tessdataFile = new File(tessdataDir, destinationFilenameBase);

        // If language data files are not present, install them
        boolean installSuccess = false;
        if(!tessdataFile.exists()){
            try {
                // Check for a file like "eng.traineddata.zip"
                installSuccess = installFromAssets(languageCode + ".zip", tessdataDir);
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
            } catch (Exception e) {
                Log.e(TAG, "Got exception", e);
            }
            if(!installSuccess){
                return false;
            }
        }else {
            installSuccess = true;
        }

        // If OSD data file is not present, install it
        String osdFilenameBase = "osd.traineddata";
        File osdFile = new File(tessdataDir, osdFilenameBase);
        boolean osdInstallSuccess = false;

        if(!osdFile.exists()){
            languageName = "orientation and script detection";
            try {
                // Check for "osd.zip"
                osdInstallSuccess = installFromAssets("osd.zip", tessdataDir);
            } catch (IOException e) {
                Log.e(TAG, "IOException", e);
            } catch (Exception e) {
                Log.e(TAG, "Got exception", e);
            }
            if(!osdInstallSuccess){
                return false;
            }
        }else {
            osdInstallSuccess = true;
        }

        // Initialize the OCR engine
        if (baseApi.init(destinationDirBase + File.separator, languageCode, ocrEngineMode)) {
            return installSuccess && osdInstallSuccess;
        }
        return false;
    }

    /**
     * Install a file from application assets to device external storage.
     *
     * @param sourceFilename
     *          File in assets to install
     * @param modelRoot
     *          Directory on SD card to install the file to
     * @return True if installZipFromAssets returns true
     * @throws IOException
     */
    private boolean installFromAssets(String sourceFilename, File modelRoot) throws IOException {
        String extension = sourceFilename.substring(sourceFilename.lastIndexOf('.'),
                sourceFilename.length());
        try {
            if (extension.equals(".zip")) {
                return installZipFromAssets(sourceFilename, modelRoot);
            } else {
                throw new IllegalArgumentException("Extension " + extension
                        + " is unsupported.");
            }
        } catch (FileNotFoundException e) {
            Log.d(TAG, "Language not packaged in application assets.");
        }
        return false;
    }

    /**
     * Unzip the given Zip file, located in application assets, into the given
     * destination file.
     *
     * @param sourceFilename
     *          Name of the file in assets
     * @param destinationDir
     *          Directory to save the destination file in
     * @return
     * @throws IOException
     */
    private boolean installZipFromAssets(String sourceFilename,File destinationDir) throws IOException {
        // Attempt to open the zip archive
        publishProgress("Uncompressing data for " + languageName + "...", "0");
        File destinationFile;
        ZipInputStream inputStream = new ZipInputStream(activity.getAssets().open(sourceFilename));

        // Loop through all the files and folders in the zip archive (but there should just be one)
        for (ZipEntry entry = inputStream.getNextEntry(); entry != null; entry = inputStream
                .getNextEntry()) {
            destinationFile = new File(destinationDir, entry.getName());

            if (entry.isDirectory()) {
                destinationFile.mkdirs();
            } else {
                // Note getSize() returns -1 when the zipfile does not have the size set
                long zippedFileSize = entry.getSize();

                // Create a file output stream
                FileOutputStream outputStream = new FileOutputStream(destinationFile);
                final int BUFFER = 8192;

                // Buffer the output to the file
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(outputStream, BUFFER);
                int unzippedSize = 0;

                // Write the contents
                int count = 0;
                Integer percentComplete = 0;
                Integer percentCompleteLast = 0;
                byte[] data = new byte[BUFFER];
                while ((count = inputStream.read(data, 0, BUFFER)) != -1) {
                    bufferedOutputStream.write(data, 0, count);
                    unzippedSize += count;
                    percentComplete = (int) ((unzippedSize / (long) zippedFileSize) * 100);
                    if (percentComplete > percentCompleteLast) {
                        publishProgress("Uncompressing data for " + languageName + "...",
                                percentComplete.toString(), "0");
                        percentCompleteLast = percentComplete;
                    }
                }
                bufferedOutputStream.close();
            }
            inputStream.closeEntry();
        }
        inputStream.close();
        return true;
    }

    /**
     * Update the dialog box with the latest incremental progress.
     *
     * @param message
     *          [0] Text to be displayed
     * @param message
     *          [1] Numeric value for the progress
     */
    @Override
    protected void onProgressUpdate(String... message) {
        super.onProgressUpdate(message);
        int percentComplete = 0;

        percentComplete = Integer.parseInt(message[1]);
        dialog.setMessage(message[0]);
        dialog.setProgress(percentComplete);
        dialog.show();
    }

    @Override
    protected void onPostExecute(Boolean result) {
        super.onPostExecute(result);

        try {
            dialog.dismiss();
        } catch (IllegalArgumentException e) {
            // Catch "View not attached to window manager" error, and continue
        }
        if (result) {
            activity.resumeOCR();
        } else {
            Toast.makeText(activity,"Error,language data unInstalled",Toast.LENGTH_SHORT).show();
        }
    }

}
