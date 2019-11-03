package com.schloesser.masterthesis.presentation.video;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.schloesser.masterthesis.R;

import java.net.ServerSocket;
import java.net.Socket;

import static com.schloesser.shared.wifidirect.SharedConstants.FRAME_HEIGHT;
import static com.schloesser.shared.wifidirect.SharedConstants.FRAME_WIDTH;
import static com.schloesser.shared.wifidirect.SharedConstants.SERVERPORT;
import static com.schloesser.shared.wifidirect.SharedConstants.TARGET_FPS;

public class VideoActivity extends AppCompatActivity {
    private TextView mStatus;
    private ImageView mCameraView;
    public MyClientThread mClient;
    public Thread clientThread;
    public Bitmap mLastFrame;

    private int face_count;
    private final Handler handler = new MyHandler(this);

    private FaceDetector mFaceDetector = new FaceDetector(FRAME_WIDTH, FRAME_HEIGHT, 10);
    private FaceDetector.Face[] faces = new FaceDetector.Face[10];
    private PointF tmp_point = new PointF();
    private Paint tmp_paint = new Paint();


    private Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mLastFrame != null) {

                            Bitmap mutableBitmap = mLastFrame.copy(Bitmap.Config.RGB_565, true);


                            face_count = mFaceDetector.findFaces(mLastFrame, faces);
                            Log.d("Face_Detection", "Face Count: " + String.valueOf(face_count));
                            Canvas canvas = new Canvas(mutableBitmap);

                            for (int i = 0; i < face_count; i++) {
                                FaceDetector.Face face = faces[i];
                                tmp_paint.setColor(Color.RED);
                                tmp_paint.setAlpha(100);
                                face.getMidPoint(tmp_point);
                                canvas.drawCircle(tmp_point.x, tmp_point.y, face.eyesDistance(),
                                        tmp_paint);
                            }

                            mCameraView.setImageBitmap(mutableBitmap);
                        }

                    }
                });
            } finally {
                handler.postDelayed(mStatusChecker, 1000 / TARGET_FPS);
            }
        }
    };

    @SuppressLint("StaticFieldLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        mCameraView = (ImageView) findViewById(R.id.camera_preview);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... unused) {
                // Background Code
                final Socket s;
                try {
                    ServerSocket ss = new ServerSocket(SERVERPORT);
                     s = ss.accept();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VideoActivity.this, "Connected to:" + s.getInetAddress().toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                    mClient = new MyClientThread(s, handler);
                    clientThread = new Thread(mClient);
                    clientThread.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }

        }.execute();
        mStatusChecker.run();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(clientThread != null) clientThread.stop();
    }
}
