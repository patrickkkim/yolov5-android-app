package org.tensorflow.lite.examples.detection;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;

public class ConnectedThread extends Thread{
    private BluetoothSocket mSocket;
    private InputStream mInStream;
    private OutputStream mOutStream;
    public static final int RESPONSE_MESSAGE = 10;
    Handler uih;

    public ConnectedThread(BluetoothSocket socket, Handler uih) {
        this.mSocket = socket;
        this.uih = uih;
        InputStream tmpIn = null;
        OutputStream tmpOut = null;
        try {
            tmpIn = socket.getInputStream();
            tmpOut = socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mInStream = tmpIn;
        mOutStream = tmpOut;

        try {
            mOutStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Log.i("Bluetooth", "Thread initialization complete.");
    }

    // Check arrival
    public void run() {
        BufferedReader br = new BufferedReader(new InputStreamReader(mInStream));
        while (mSocket != null) {
            try {
                String response = br.readLine();
                Message msg = new Message();
                msg.what = RESPONSE_MESSAGE;
                msg.obj = response;
                uih.sendMessage(msg);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Write to module
    public void write(byte[] bytes) {
        try {
            Log.i("Bluetooth", "Writing to bluetooth module");
            mOutStream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Send stop connection
    public void cancel() {
        try {
            mSocket.close();
            mSocket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
