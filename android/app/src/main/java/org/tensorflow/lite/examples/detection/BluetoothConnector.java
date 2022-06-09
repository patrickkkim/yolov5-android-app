package org.tensorflow.lite.examples.detection;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothConnector extends Application {
    private static BluetoothConnector instance;

    private BluetoothAdapter adapter;
    private Set<BluetoothDevice> pairedDevices;
    private ConnectedThread btt = null;
    private static final String MAC = "B8:27:EB:51:A9:2C";
    private static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private TextToSpeech tts;

    private BluetoothConnector(Context context) {
        tts = TextToSpeech.getInstance(this);
        adapter = BluetoothAdapter.getDefaultAdapter();

        boolean result = false;
        while (!result) {
            result = connectDevice();
        }
    }

    public static BluetoothConnector getInstance(Context context) {
        if (instance == null) {
            instance = new BluetoothConnector(context);
        }
        return instance;
    }


//    private void on() {
//        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//        startActivityForResult(turnOn, 0);
////        pairedDevices = adapter.getBondedDevices();
////        System.out.println(pairedDevices);
//    }

    private void off() {
        if (btt != null) {
            btt.cancel();
            btt = null;
        }
    }

    private boolean connectDevice() {
        BluetoothDevice mDevice;
        BluetoothSocket mSocket = null;
        BluetoothSocket tmp = null;
        Handler mHandler;

        try {
            mDevice = adapter.getRemoteDevice(MAC);
            tmp = mDevice.createRfcommSocketToServiceRecord(DEVICE_UUID);
            mSocket = tmp;
            mSocket.connect();
            Log.i("Bluetooth", "Connected to " + mDevice.getName() + " " + mDevice.getAddress());
        } catch (IOException | NullPointerException e1) {
            e1.printStackTrace();
            try {
                mSocket.close();
            } catch (IOException | NullPointerException e2) {
                e2.printStackTrace();
            }
            return false;
        }

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                    String txt = (String) msg.obj;
                    processScanData(txt);
                }
            }
        };

        btt = new ConnectedThread(mSocket, mHandler);
        btt.start();
        return true;
    }

    private void processScanData(String dataStr) {
        List<String> dataList = new ArrayList<>();

        if (dataStr.charAt(0) == '[') {
            dataStr = dataStr.substring(1, dataStr.length() - 1);
            dataList = Arrays.asList(dataStr.split(", "));

            float minDistance = 99.9f;

            int windowSize = 16;
            for (int i = 0; i < dataList.size(); i+=windowSize) {
                int infCount = 0;
                for (int j = i; j < i+windowSize && j < dataList.size(); ++j) {
                    if (dataList.get(j).equals("inf")) {
                        infCount += 1;
                    }
                }

                if (infCount < (windowSize / 2)) {
                    for (int j = i; j < i+windowSize && j < dataList.size(); ++j) {
                        if (dataList.get(j).equals("inf")) { continue; }
                        if (minDistance < 0.3) { continue; }
                        float distance = Float.parseFloat(dataList.get(j));
                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                    }
                }
            }

            if (minDistance > 0.3 && minDistance < 1.2) {
                tts.makeBeep();
            }
//            tts.readText(direction + " " + String.format("%.2f", minDistance));
        }

        // 223.39 pixels width
        // Log.d("Bluetooth Data", String.valueOf(dataList.size()));
    }
}
