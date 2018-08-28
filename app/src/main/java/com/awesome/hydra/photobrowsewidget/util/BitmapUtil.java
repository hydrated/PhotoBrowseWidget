package com.awesome.hydra.photobrowsewidget.util;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.view.View;

/**
 * Created by Daniel Lin on 5/10/16.
 */
public class BitmapUtil {

    public static final int MAX_PARSE_FILE_SIZE = 10 * 1048576;

    public static class BitmapDecoder {
        private enum Source {
            BYTE_ARRAY,
            PATH,
            RESOURCE_ID
        }

        private Source source;
        private String dataAsPath = null;
        private int dataAsResourceId;
        private byte[] dataAsByteArray = null;
        private int targetWidth;
        private int targetHeight;
        private Resources resources;

        public Bitmap decodeSampledBitmap(String path, int reqWidth, int reqHeight) {
            source = Source.PATH;
            this.dataAsPath = path;
            targetWidth = reqWidth;
            targetHeight = reqHeight;
            return decodeSampledBitmap();
        }

        public Bitmap decodeSampledBitmap(byte[] data, int reqWidth, int reqHeight) {
            source = Source.BYTE_ARRAY;
            this.dataAsByteArray = data;
            targetWidth = reqWidth;
            targetHeight = reqHeight;
            return decodeSampledBitmap();
        }

        public Bitmap decodeSampledBitmap(int resourceId, Resources resources, int reqWidth, int reqHeight) {
            source = Source.RESOURCE_ID;
            this.dataAsResourceId = resourceId;
            this.resources = resources;
            targetWidth = reqWidth;
            targetHeight = reqHeight;
            return decodeSampledBitmap();
        }

        private Bitmap decodeSampledBitmap() {
            // First decode with inJustDecodeBounds=true to check dimensions
            final BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            options.inScaled = false;

            switch (source) {
                case PATH:
                    BitmapFactory.decodeFile(dataAsPath, options);
                    break;
                case BYTE_ARRAY:
                    BitmapFactory.decodeByteArray(dataAsByteArray, 0, dataAsByteArray.length, options);
                    break;
                case RESOURCE_ID:
                    BitmapFactory.decodeResource(resources, dataAsResourceId, options);
                    break;
                default:
                    // Missing a case ?
                    return null;
            }

            // Calculate inSampleSize
            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight);

            // Decode bitmap with inSampleSize set
            options.inJustDecodeBounds = false;

            switch (source) {
                case PATH:
                    return BitmapFactory.decodeFile(dataAsPath, options);
                case BYTE_ARRAY:
                    return BitmapFactory.decodeByteArray(dataAsByteArray, 0, dataAsByteArray.length, options);
                case RESOURCE_ID:
                    return BitmapFactory.decodeResource(resources, dataAsResourceId, options);
                default:
                    // Missing a case ?
                    return null;
            }
        }

        public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
            // Raw height and width of image
            final int height = options.outHeight;
            final int width = options.outWidth;
            int inSampleSize = 1;

            if (height > reqHeight || width > reqWidth) {

                final int halfHeight = height / 2;
                final int halfWidth = width / 2;

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) > reqHeight
                        && (halfWidth / inSampleSize) > reqWidth) {
                    inSampleSize *= 2;
                }
            }

            return inSampleSize;
        }
    }

    /**
     * Create bitmap with whose dimension is equal to the view's measured dimension
     * @param view
     * @return
     */
    public static Bitmap createBitmapFromView(View view) {
        Bitmap bitmap  = createBitmapFromView(view,  view.getMeasuredWidth(), view.getMeasuredHeight());
        return bitmap;
    }

    public static Bitmap createBitmapFromView(View view, int bitmapWidth, int bitmapHeight) {
        Bitmap bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);

        return bitmap;
    }

    /**
     * Scale bitmap to size
     *
     * @param bitmap src bitmap
     * @param scale
     * @return the scaled bitmap
     */
    public static Bitmap scaleBitmap(Bitmap bitmap, Double scale) {
        Double w = bitmap.getWidth() * scale;
        Double h = bitmap.getHeight() * scale;
        return Bitmap.createScaledBitmap(bitmap, w.intValue(), h.intValue(), true);
    }

    /**
     * Get the bytesize of the give bitmap
     *
     * @param bitmap src bitmap
     * @return the bytesize of the bitmap
     */
    public static int byteSizeOf(Bitmap bitmap) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return bitmap.getAllocationByteCount();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            return bitmap.getByteCount();
        } else {
            return bitmap.getRowBytes() * bitmap.getHeight();
        }
    }
}
