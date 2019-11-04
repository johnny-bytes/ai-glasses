package com.schloesser.masterthesis.presentation.video;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.FaceDetector;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import static com.schloesser.shared.wifidirect.SharedConstants.HEADER_END;
import static com.schloesser.shared.wifidirect.SharedConstants.HEADER_START;

public class MyClientThread implements Runnable {
    private Socket mSocket;
    private Handler mHandler;
    private Boolean mRunFlag = true;
    final private String TAG = "MyClientThread";
    private BitmapFactory.Options bitmap_options = new BitmapFactory.Options();



    public MyClientThread(Socket socket, Handler handler) throws IOException {
        this.mSocket = socket;
        this.mHandler = handler;
        bitmap_options.inPreferredConfig = Bitmap.Config.RGB_565;
        //br = new BufferedReader(new InputStreamReader(s.getInputStream()));
    }

    @Override
    public void run() {
        try {
            InputStream inStream = null;
            try {
                inStream = mSocket.getInputStream();
            } catch (Exception e) {
                e.printStackTrace();
            }
            DataInputStream is = new DataInputStream(inStream);
            while (mRunFlag) {
                try {

                        if (is.readUTF().equals(HEADER_START)) {

                            int imgLength = is.readInt();

                            if (!is.readUTF().equals(HEADER_END)) {
                                Log.d(TAG, "Header End Tag not present.");
                            }

                            byte[] buffer = new byte[imgLength];
                            int len = 0;
                            while (len < imgLength) {
                                len += is.read(buffer, len, imgLength - len);
                            }
                            Message m = mHandler.obtainMessage();
                            m.obj = BitmapFactory.decodeByteArray(buffer, 0, buffer.length,bitmap_options);
                            if (m.obj != null) {
                                mHandler.sendMessage(m);
                            } else {
                                System.out.println("Decode Failed");
                            }
                        }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

