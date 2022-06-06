package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Iterator;
// import org.ros.RosCore;

public class USBActivity extends AppCompatActivity {
    TextView deviceListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usb);
        deviceListView = (TextView) findViewById(R.id.textViewDeviceList);

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        UsbAccessory accessory = (UsbAccessory) getIntent().getParcelableExtra(UsbManager.EXTRA_ACCESSORY);

        UsbAccessory[] accessoryList = manager.getAccessoryList();
        if (accessoryList != null && accessoryList.length != 0) {
            for (UsbAccessory acc : accessoryList) {
                deviceListView.append(acc.toString());
                Log.d("USB Device", acc.toString());
            }
        } else {
            Toast.makeText(this, "Accessory List is empty...", Toast.LENGTH_SHORT).show();
        }
    }
}