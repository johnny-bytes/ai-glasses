package com.schloesser.masterthesis.presentation.video;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Handler;
import android.os.Message;

import com.schloesser.masterthesis.MainActivity;

import java.lang.ref.WeakReference;

/**
 * Created by Alvin on 2016-05-20.
 */
class MyHandler extends Handler {
    private final WeakReference<VideoActivity> mActivity;

    MyHandler(VideoActivity activity) {
        mActivity = new WeakReference<VideoActivity>(activity);
    }

    @Override
    public void handleMessage(Message msg) {
        VideoActivity activity = mActivity.get();
        if (activity != null) {
            try {
                activity.mLastFrame = rotateImage((Bitmap) msg.obj, 90);
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.handleMessage(msg);
        }
    }

    private static Bitmap rotateImage(Bitmap source, float angle) {
        if (source != null) {
            Bitmap retVal;

            Matrix matrix = new Matrix();
            matrix.postRotate(angle);
            retVal = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
            source.recycle();
            return retVal;
        }
        return null;
    }
}