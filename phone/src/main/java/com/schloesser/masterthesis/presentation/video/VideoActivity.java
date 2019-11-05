package com.schloesser.masterthesis.presentation.video;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.media.FaceDetector;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.schloesser.masterthesis.R;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import static com.schloesser.shared.wifidirect.SharedConstants.FRAME_HEIGHT;
import static com.schloesser.shared.wifidirect.SharedConstants.FRAME_WIDTH;
import static com.schloesser.shared.wifidirect.SharedConstants.HEADER_END;
import static com.schloesser.shared.wifidirect.SharedConstants.HEADER_START;
import static com.schloesser.shared.wifidirect.SharedConstants.SERVERPORT;
import static com.schloesser.shared.wifidirect.SharedConstants.TARGET_FPS;


public class VideoActivity extends AppCompatActivity {
    private ImageView mCameraView;
    public MyClientThread mClient;
    public Thread clientThread;
    public Bitmap mLastFrame;

    private final Handler handler = new MyHandler(this);

    private FaceDetector mFaceDetector = new FaceDetector(FRAME_WIDTH, FRAME_HEIGHT, 10);
    private FaceDetector.Face[] faces = new FaceDetector.Face[10];
    private PointF tmp_point = new PointF();
    private Paint tmp_paint = new Paint();
    private Socket mSocket;
    private DataOutputStream outputStream;

    private Runnable mStatusChecker = new Runnable() {
        @SuppressLint("StaticFieldLeak")
        @Override
        public void run() {
            try {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mLastFrame != null) {

                            Bitmap mutableBitmap = mLastFrame.copy(Bitmap.Config.RGB_565, true);

                            final int face_count = mFaceDetector.findFaces(mLastFrame, faces);

                            new AsyncTask<Void, Void, Void>() {

                                @Override
                                protected Void doInBackground(Void... unused) {
                                    try {
                                        outputStream.writeUTF(HEADER_START);
                                        outputStream.writeInt(face_count);
                                        outputStream.writeUTF(HEADER_END);
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return null;
                                }
                            }.execute();

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
        mCameraView = findViewById(R.id.camera_preview);

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... unused) {
                // Background Code
                try {
                    ServerSocket ss = new ServerSocket(SERVERPORT);
                    mSocket = ss.accept();

                    try {
                        outputStream = new DataOutputStream(mSocket.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(VideoActivity.this, "Connected to:" + mSocket.getInetAddress().toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                    mClient = new MyClientThread(mSocket, handler);
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
        if (clientThread != null) clientThread.stop();
    }
}
