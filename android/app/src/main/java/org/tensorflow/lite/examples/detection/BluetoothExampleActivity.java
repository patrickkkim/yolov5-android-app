package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class BluetoothExampleActivity extends AppCompatActivity {
    private BluetoothAdapter adapter;
    private Set<BluetoothDevice> pairedDevices;
    private ConnectedThread btt = null;
    private static final String MAC = "B8:27:EB:51:A9:2C";
    private static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    private TextView response;
    private Button btn_connect, btn_stop;

    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_example);
        response = (TextView) findViewById(R.id.textViewResult);
        btn_connect = (Button) findViewById(R.id.buttonBluetoothConnect);
        btn_stop = (Button) findViewById(R.id.buttonBluetoothStop);

        tts = TextToSpeech.getInstance(this);

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                connectDevice();
                Toast.makeText(BluetoothExampleActivity.this, "연결을 완료했습니다", Toast.LENGTH_SHORT).show();
            }
        });
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                off();
                Toast.makeText(BluetoothExampleActivity.this, "연결을 해제했습니다", Toast.LENGTH_SHORT).show();
            }
        });

        adapter = BluetoothAdapter.getDefaultAdapter();
        on();
    }

    @Override
    protected void onDestroy() {
        off();
        super.onDestroy();
    }

    private void on() {
        Intent turnOn = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(turnOn, 0);
//        pairedDevices = adapter.getBondedDevices();
//        System.out.println(pairedDevices);
    }

    private void off() {
        if (btt != null) {
            btt.cancel();
            btt = null;
        }
    }

    private void connectDevice() {
        BluetoothDevice mDevice;
        BluetoothSocket mSocket = null;
        BluetoothSocket tmp = null;
        Handler mHandler;

        mDevice = adapter.getRemoteDevice(MAC);
        try {
            tmp = mDevice.createRfcommSocketToServiceRecord(DEVICE_UUID);
            mSocket = tmp;
            mSocket.connect();
            Log.i("Bluetooth", "Connected to " + mDevice.getName() + " " + mDevice.getAddress());
        } catch (IOException e1) {
            e1.printStackTrace();
            try {
                mSocket.close();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }

        mHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                if(msg.what == ConnectedThread.RESPONSE_MESSAGE){
                    String txt = (String) msg.obj;
                  
                    processScanData(txt);
                    if(response.getText().toString().length() >= 100){
                        response.setText("");
                        response.append(txt);
                    } else {
                        response.append("\n" + txt);
                    }
                }
            }
        };

        btt = new ConnectedThread(mSocket, mHandler);
        btt.start();
    }

    private void processScanData(String dataStr) {
        List<String> dataList = new ArrayList<>();

        if (dataStr.charAt(0) == '[') {
            dataStr = dataStr.substring(1, dataStr.length() - 1);
            dataList = Arrays.asList(dataStr.split(", "));

            float minDistance = 99.9f;

            int windowSize = 3;
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
                        float distance = Float.parseFloat(dataList.get(j));
                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                    }
                }
            }

            if (minDistance > 0.1 && minDistance < 0.8) {
                tts.makeBeep();
            }
//            tts.readText(direction + " " + String.format("%.2f", minDistance));
        }

        // 223.39 pixels width
        // Log.d("Bluetooth Data", String.valueOf(dataList.size()));
    }
}