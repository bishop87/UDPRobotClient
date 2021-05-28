package it.bishop87.udpclient;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.NetworkOnMainThreadException;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Locale;

import io.github.controlwear.virtual.joystick.android.JoystickView;

public class MainActivity extends AppCompatActivity {


    EditText editTextAddress, editTextPort;
    Button btnConnect, btnDisconnect, btnSendtest;
    TextView textViewState, textViewLog;

    private InetAddress dstAddr;
    private int dstPort;

    private DatagramSocket socket;
    HandlerThread handlerThread;
    Handler handler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnConnect = (Button) findViewById(R.id.connect);
        btnDisconnect = (Button) findViewById(R.id.disconnect);
        btnSendtest = (Button) findViewById(R.id.sendtest);

        editTextAddress = (EditText) findViewById(R.id.address);
        editTextPort = (EditText) findViewById(R.id.port);
        textViewState = (TextView)findViewById(R.id.state);
        textViewLog = (TextView)findViewById(R.id.log);

        JoystickView joystick = (JoystickView) findViewById(R.id.joystickView);
        joystick.setOnMoveListener(new JoystickView.OnMoveListener() {
            @Override
            public void onMove(final int angle, final int strength) {
                Log.i("joypad", "send: " + String.format("angle: %03d - strength: %03d", angle, strength));

                calcMotorSpeed(angle, strength);

                if (socket != null && !socket.isClosed()){
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (socket != null && !socket.isClosed()) {
                                try {
                                    byte[] buf = calcMotorSpeed(angle, strength);
                                    DatagramPacket packet = new DatagramPacket(buf, buf.length, dstAddr, dstPort);
                                    socket.send(packet);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    });
                }
            }
        },200);
    }

    public void btnConnectClick(View view) {
        textViewState.setText("connecting...");
        try {
            socket = new DatagramSocket();
        } catch (SocketException e) {
            e.printStackTrace();
            Toast.makeText(MainActivity.this, "SocketException", Toast.LENGTH_LONG).show();
            return;
        }

        handlerThread = new HandlerThread("MyHandlerThread");
        handlerThread.start();
        Looper looper = handlerThread.getLooper();
        handler = new Handler(looper);

        handler.post(new Runnable(){
            @Override
            public void run() {
                Log.i("btnSendtestClick", "connect...");

                try {
                    dstAddr = InetAddress.getByName(editTextAddress.getText().toString());
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "UnknownHostException", Toast.LENGTH_LONG).show();
                }
                dstPort = Integer.parseInt(editTextPort.getText().toString());

                //check connetion
                try {
                    byte[] buf = "hello".getBytes();
                    DatagramPacket packet = new DatagramPacket(buf, buf.length, dstAddr, dstPort);
                    socket.send(packet);
                    // get response
                    packet = new DatagramPacket(buf, buf.length);
                    socket.receive(packet);
                    String line = new String(packet.getData(), 0, packet.getLength());

                    if(line.equals("ack")){
                        textViewState.setText("connected!");
                    }else {
                        textViewState.setText("not connected");
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NetworkOnMainThreadException e){
                    e.printStackTrace();
                }
            }});
        btnConnect.setEnabled(false);
        btnSendtest.setEnabled(true);
        btnDisconnect.setEnabled(true);
    }

    public void btnSendtestClick(View view) {
        handler.post(new Runnable(){
            @Override
            public void run() {
                Log.i("btnSendtestClick", "send...");
                if(socket != null && !socket.isClosed()) {
                    try {
                        byte[] buf = "udp remote controller".getBytes();
                        DatagramPacket packet = new DatagramPacket(buf, buf.length, dstAddr, dstPort);
                        socket.send(packet);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void btnDisconnectClick(View view) {
        handler.post(new Runnable(){
            @Override
            public void run() {
                Log.i("btnSendtestClick", "close...");
                if(socket != null){
                    socket.close();
                }
            }
        });
        btnConnect.setEnabled(true);
        btnSendtest.setEnabled(false);
        btnDisconnect.setEnabled(false);
    }


    private byte[] calcMotorSpeed(int angle, int strength){
        int in1 = 0, in2 = 0, in3 = 0, in4 = 0;
        int motorSpeedA;
        int motorSpeedB;

        // Y-axis used for forward and backward control
        if (angle <= 360 && angle > 180) {
            in1 = 1; in2 = 0; // Set Motor A backward
            in3 = 1; in4 = 0; // Set Motor B backward
            int yMapped = (int)(Math.sin(Math.toRadians(angle)) * -strength);
            motorSpeedA = map(yMapped, 0, 100, 0, 255);
            motorSpeedB = map(yMapped, 0, 100, 0, 255);
        }
        else if (angle >= 0 && angle <=180) {
            in1 = 0; in2 = 1; // Set Motor A forward
            in3 = 0; in4 = 1; // Set Motor B forward
            int yMapped = (int)(Math.sin(Math.toRadians(angle)) * strength);
            motorSpeedA = map(yMapped, 0, 100, 0, 255);
            motorSpeedB = map(yMapped, 0, 100, 0, 255);
        }
        else {// If joystick stays in middle the motors are not moving
            motorSpeedA = 0;
            motorSpeedB = 0;
        }

        // X-axis used for left and right control
        if (angle >= 90 && angle <= 270) { //LEFT
            int xMapped = (int)(Math.cos(Math.toRadians(angle)) * -strength);
            motorSpeedA = Math.max(motorSpeedA - xMapped, 0);
            motorSpeedB = Math.min(motorSpeedB + xMapped, 255);
        }
        if ((angle < 90 && angle >= 0) || ( angle > 270 && angle <= 360)) { //RIGHT
            int xMapped = (int)(Math.cos(Math.toRadians(angle)) * strength);
            motorSpeedA = Math.min(motorSpeedA + xMapped, 255);
            motorSpeedB = Math.max(motorSpeedB - xMapped, 0);
        }
        // Prevent buzzing at low speeds (Adjust according to your motors. My motors couldn't start moving if PWM value was below value of 70)
        if (motorSpeedA < 65) { motorSpeedA = 0; }
        if (motorSpeedB < 65) { motorSpeedB = 0; }

        String output = String.format(Locale.getDefault(),"%01d,%01d,%01d,%01d,%03d,%03d", in1, in2, in3, in4, motorSpeedA, motorSpeedB);
        Log.i("calcMotorSpeed", output);
        textViewLog.setText(output);
        return output.getBytes();
    }


    //re-maps a number from one range to another
    int map(int x, int in_min, int in_max, int out_min, int out_max)
    {
        return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
    }

}
