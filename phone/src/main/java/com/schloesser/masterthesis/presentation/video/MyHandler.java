package com.schloesser.masterthesis.presentation.video;

import android.graphics.Bitmap;
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
                activity.mLastFrame = (Bitmap) msg.obj;
            } catch (Exception e) {
                e.printStackTrace();
            }
            super.handleMessage(msg);
        }
    }
}