package it.bishop87.udpclient;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class UdpClientThread2 extends Thread {
    private Handler mHandler;

    public static final int SEND_CODE = 1;
    public static final int QUIT_CODE = 2;

    @Override
    public void run() {
        if (Looper.myLooper()==null)
            Looper.prepare();

        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == SEND_CODE) {
                    // Send
                    Log.i("udpthread", "send...");
                } else if (msg.what == QUIT_CODE) {
                    //Looper.quitSafely();
                    Log.i("udpthread", "quit...");
                    this.getLooper().quitSafely();
                    //Looper.myLooper().quitSafely();
                }
            }
        };
        Looper.loop();
    }

    public Handler getThreadHandler() {
        return mHandler;
    }
}