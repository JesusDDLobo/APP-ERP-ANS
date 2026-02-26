package com.example.app_ans.core.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class FileStorageUtils {
    private static final String TAG = "FileStorageUtils";
    private static final String ADVANCES_DIR = "pending_advances";

    public static boolean isValidFile(Context context, Uri uri) {
        if (uri == null) return false;
        try {
            // Check if we can open it
            try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                if (is == null) return false;
            }

            // Check size (e.g., 20MB limit)
            long size = -1;
            if ("content".equals(uri.getScheme())) {
                try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex != -1) size = cursor.getLong(sizeIndex);
                    }
                }
            }

            // 20MB limit
            return size == -1 || size <= 20 * 1024 * 1024;
        } catch (Exception e) {
            Log.e(TAG, "Error validating file: " + uri, e);
            return false;
        }
    }

    public static List<String> saveFilesLocally(Context context, List<Uri> uris) {
        List<String> savedPaths = new ArrayList<>();
        File dir = new File(context.getFilesDir(), ADVANCES_DIR);
        if (!dir.exists()) dir.mkdirs();

        for (Uri uri : uris) {
            try {
                String fileName = getFileName(context, uri);
                String mimeType = context.getContentResolver().getType(uri);
                File destFile = new File(dir, System.currentTimeMillis() + "_" + fileName);
                
                if (mimeType != null && mimeType.startsWith("image/")) {
                    compressAndSaveImage(context, uri, destFile);
                } else {
                    copyFile(context, uri, destFile);
                }
                savedPaths.add(destFile.getAbsolutePath());
            } catch (Exception e) {
                Log.e(TAG, "Error saving file: " + uri, e);
            }
        }
        return savedPaths;
    }

    private static void compressAndSaveImage(Context context, Uri uri, File destFile) throws Exception {
        // First decode with inJustDecodeBounds=true to check dimensions
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            BitmapFactory.decodeStream(is, null, options);
        }

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, 1920, 1920);
        options.inJustDecodeBounds = false;

        // Decode bitmap with inSampleSize set
        Bitmap bitmap;
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            bitmap = BitmapFactory.decodeStream(is, null, options);
        }

        if (bitmap == null) {
            copyFile(context, uri, destFile);
            return;
        }

        // Compress and save
        try (FileOutputStream fos = new FileOutputStream(destFile)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
        }
        bitmap.recycle();
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private static void copyFile(Context context, Uri uri, File destFile) throws Exception {
        try (InputStream is = context.getContentResolver().openInputStream(uri);
             FileOutputStream fos = new FileOutputStream(destFile)) {
            if (is == null) throw new Exception("Could not open input stream for " + uri);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = is.read(buffer)) != -1) {
                fos.write(buffer, 0, read);
            }
        } catch (SecurityException se) {
            Log.e(TAG, "SecurityException reading file from " + uri + ". Do we have permissions?", se);
            throw new Exception("Error de permisos: No se pudo leer el archivo de la fuente original.");
        }
    }

    public static File getCompressedFile(Context context, File file) {
        String name = file.getName().toLowerCase();
        if (!name.endsWith(".jpg") && !name.endsWith(".jpeg") && !name.endsWith(".png")) {
            return file;
        }

        try {
            File tempFile = new File(context.getCacheDir(), "temp_" + file.getName());
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);

            options.inSampleSize = calculateInSampleSize(options, 1920, 1920);
            options.inJustDecodeBounds = false;

            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap == null) return file;

            try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            }
            bitmap.recycle();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error compressing file: " + file.getPath(), e);
            return file;
        }
    }

    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri != null && "content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index != -1) result = cursor.getString(index);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error querying file name for uri: " + uri, e);
            }
        }
        if (result == null && uri != null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    public static void deleteFile(String path) {
        File file = new File(path);
        if (file.exists()) file.delete();
    }

    public static File fromUri(Context context, Uri uri) {
        try {
            String fileName = getFileName(context, uri);
            File tempFile = new File(context.getCacheDir(), "upload_" + System.currentTimeMillis() + "_" + fileName);
            copyFile(context, uri, tempFile);
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "Error creating temp file from uri: " + uri, e);
            return null;
        }
    }
}
