package com.awesome.hydra.photobrowsewidget.util;


import android.app.Activity;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import bolts.Task;
import bolts.TaskCompletionSource;

public class GetPictureUtil {

    public static int REQUEST_CAMERA = 0x1;
    public static int REQUEST_GALLERY = 0x2;
    private static String KEY_EXPECTED_PICTURE_PATH = "expectedPicturePath";

    /**
     * usage:
     * GetPicture getPicture = new GetPicture(Activity instance, callback);
     * in Activity.onActivityResult(...): call getPicture.onActivityResult(...)
     * in Activity.onRestoreInstanceState(...): call getPicture.onRestoreInstanceState(...)
     * in Activity.onSaveInstanceState(...): call getPicture.onSaveInstanceState(...)
     * getPicture.fromSource(CAMERA);
     */
    public static class GetPicture {
        private GetPictureCallback callback;
        private Activity activity;


        public enum PictureSource {
            CAMERA,
            GALLERY
        }

        public interface GetPictureCallback {
            void onPictureAvailable(Bitmap bitmap);

            void onCancel();

            void onError(Exception e);
        }

        public GetPicture(Activity activity, GetPictureCallback callback) {
            this.activity = activity;
            this.callback = callback;
        }

        public void fromSource(PictureSource source) {
            try {
                switch (source) {
                    case CAMERA:
                        selectPictureWithCamera(REQUEST_CAMERA);
                        break;
                    case GALLERY:
                        selectPictureFromGallery(REQUEST_GALLERY);
                        break;
                }
            } catch (Exception e) {
                callback.onError(e);
            }
        }

        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (resultCode != Activity.RESULT_OK) {
                callback.onCancel();
                return;
            }

            if (requestCode == REQUEST_CAMERA) {
                handlePictureFromCamera(expectedPicturePath);
            } else if (requestCode == REQUEST_GALLERY) {
                handlePictureFromGallery(data);
            }
        }

        public void onSaveInstanceState(Bundle outState) {
            outState.putString(KEY_EXPECTED_PICTURE_PATH, expectedPicturePath);
        }

        public void onRestoreInstanceState(Bundle savedInstanceState) {
            if (savedInstanceState.containsKey(KEY_EXPECTED_PICTURE_PATH)) {
                expectedPicturePath = savedInstanceState.getString(KEY_EXPECTED_PICTURE_PATH);
            }
        }

        private String expectedPicturePath = null;

        private void selectPictureWithCamera(int requestCode) throws IOException {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (intent.resolveActivity(activity.getPackageManager()) != null) {
                File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
                if (!storageDir.exists()) {
                    storageDir.mkdirs();
                }
                File imageFile = new File(storageDir, "profile_" + System.currentTimeMillis() + ".jpeg");
                this.expectedPicturePath = imageFile.getAbsolutePath();
                intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(imageFile));
                Uri photoURI;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    photoURI = FileProvider.getUriForFile(activity,
                            activity.getApplicationContext().getPackageName()+".provider",
                            imageFile);
                } else {
                    photoURI = Uri.fromFile(imageFile);
                }
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                activity.startActivityForResult(intent, requestCode);
            }
        }

        private void selectPictureFromGallery(int requestCode) {
            Intent intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            activity.startActivityForResult(
                    Intent.createChooser(intent, "Select File"),
                    requestCode);
        }

        private void handlePictureFromCamera(String imagePath) {
            handleImagePath(imagePath);
        }

        private void handlePictureFromGallery(Intent data) {

            Uri selectedImageUri = data.getData();

            if (activity == null || activity.isFinishing() || selectedImageUri == null) {
                callback.onError(new Exception("Activity is finishing or null"));
                return;
            }

            String[] projection = {MediaStore.MediaColumns.DATA};
            CursorLoader cursorLoader = new CursorLoader(activity, selectedImageUri, projection, null, null, null);
            Cursor cursor = cursorLoader.loadInBackground();
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            if (!cursor.moveToFirst()) {
                return;
            }
            String imagePath = cursor.getString(column_index);
            handleImagePath(imagePath);
        }

        private void handleImagePath(String imagePath) {
            Bitmap bitmap = new BitmapUtil.BitmapDecoder().decodeSampledBitmap(imagePath, 300, 300);

            int orientation = ExifInterface.ORIENTATION_NORMAL;
            try {
                orientation = getImageOrientation(imagePath);
            } catch (Exception e) {
                e.printStackTrace();
            }

            Matrix matrix = new Matrix();
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_180:
                    matrix.preRotate(180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    matrix.preRotate(270);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    matrix.preRotate(90);
                    break;
                case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                    matrix.preRotate(180);
                    break;
            }

            Bitmap bitmapRotated = null;

            if(bitmap != null) {
                bitmapRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            }
            
            handleBitmap(bitmapRotated);
        }

        private int getImageOrientation(String filename) throws Exception {
            ExifInterface exif = new ExifInterface(filename);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);
            return orientation;
        }

        private void handleBitmap(Bitmap bitmap) {
            callback.onPictureAvailable(bitmap);
        }
    }

    /**
     * Get List Photo device gallery
     *
     * @param ctx
     * @return List gallery Photo paths
     */
    public static List<String> getGalleryList(Context ctx, Date checkInTime, Date checkOutTime) {
        List<String> latestImages = getCameraImages(ctx);
        List<String> result = new ArrayList<>();
        for (String path: latestImages) {
            if (isValidImage(path) && isValidTime(path, checkInTime, checkOutTime))
                result.add(path);
        }
        return result;
    }

    public static Task<List<String>> getAllGalleryList(final Context ctx) {
        if(ctx == null) return Task.forResult(null);

        final TaskCompletionSource<List<String>> tcs = new TaskCompletionSource<>();
        Task.callInBackground(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                List<String> latestImages = getCameraImages(ctx);
                List<String> result = new ArrayList<>();
                for (String path : latestImages) {
                    if (isValidImage(path)) {
                        result.add(path);
                    }
                }
                tcs.setResult(result);
                return null;
            }
        });
        return tcs.getTask();
    }

    private static List<String> getCameraImages(Context context) {
        if(context == null) return new ArrayList<>();

        final String[] projection = new String[]{
                MediaStore.Images.ImageColumns._ID,
                MediaStore.Images.ImageColumns.DATA,
                MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                MediaStore.Images.ImageColumns.DATE_TAKEN,
                MediaStore.Images.ImageColumns.MIME_TYPE
        };
        final Cursor cursor = context.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC");
        List<String> result = new ArrayList<>(cursor != null ? cursor.getCount() : 0);
        assert cursor != null;
        if (cursor.moveToFirst()) {
            final int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            do {
                final String data = cursor.getString(dataColumn);
                result.add(data);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    private static boolean isValidImage(String path) {
        return path.toUpperCase().contains(".JPG") || path.toUpperCase().contains(".JPGE") || path.toUpperCase().contains(".PNG") || path.toUpperCase().contains(".GIF") || path.toUpperCase().contains(".BMP");
    }

    public static boolean isValidTime (String path, Date checkInTime, Date checkOutTime) {
        File imgFile = new File(path);
        long createDate = imgFile.lastModified();
        long inTime = checkInTime == null ? 0 : checkInTime.getTime();
        long outTime = checkOutTime == null ? 0 : checkOutTime.getTime();

        if (inTime > 0 && createDate < inTime)
            return false;

        if (outTime > 0 && createDate > outTime)
            return false;

        if (inTime == 0 && outTime == 0)
            return false;

        return true;
    }
}