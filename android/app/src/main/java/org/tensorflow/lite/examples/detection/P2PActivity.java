package org.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

public class P2PActivity extends AppCompatActivity {
    private final IntentFilter intentFilter = new IntentFilter();
    private WifiP2pManager.Channel channel;
    private WifiP2pManager manager;
    private BReceiver receiver;
    private List<WifiP2pDevice> peers = new ArrayList<>();
    private final WifiP2pManager.PeerListListener peerListListener = peerList -> {
        Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
        if (!refreshedPeers.equals(peers)) {
            peers.clear();
            peers.addAll(refreshedPeers);
            Log.i("Wifi Direct", "Devices found: " + peers); // Notify change
            this.connect();
        }
        if (peers.size() == 0) {
            Log.d("Wifi Direct", "No devices found");
            return;
        }
    };
    private final WifiP2pManager.ConnectionInfoListener connectionInfoListener = wifiP2pInfo -> {
        Log.i("Wifi Direct", wifiP2pInfo.toString());
    };

    // Broadcast receiver class for P2P
    private class BReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                // If Wifi P2P mode is enabled or not?
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    // enabled
                    Log.i("Wifi Direct", "Wifi P2P is available");
                } else {
                    // not enabled
                    Log.e("Wifi Direct", "Wifi P2P is not available");
                }
            } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                // If peer list has changed?
                if (manager != null) {
                    manager.requestPeers(channel, peerListListener);
                }
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {
                // If connection state changed?
                if (manager == null) return;

                NetworkInfo networkInfo = (NetworkInfo) intent.getParcelableExtra(
                        WifiP2pManager.EXTRA_NETWORK_INFO
                );
                if (networkInfo.isConnected()) {
                    // Connection success
                    manager.requestConnectionInfo(channel, connectionInfoListener);
                } else {
                    manager.requestGroupInfo(channel, group -> {
                        if (group != null) {
                            String groupPassword = group.getPassphrase();
                            Log.i("Wifi Direct", "Password: " + groupPassword);
                        } else {
                            Log.e("Wifi Direct", "No groups found");
                        }
                    });
                }
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // If device details has changed?
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_p2p);

        // Indicates a change in the Wi-Fi P2P status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        // Indicates the state of Wi-Fi P2P connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);

        this.initSearchDevice();
    }

    private void initSearchDevice() {
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // Discovery initiation is successful
                Log.i("Wifi Direct", "Discovery initiated");
            }
            @Override
            public void onFailure(int reasonCode) {
                // Discovery initiation failed
                Log.e("Wifi Direct", "Discovery failed(reason code: "
                        + String.valueOf(reasonCode) + ")");
            }
        });
    }

    private void connect() {
        WifiP2pDevice device = peers.get(0);
        WifiP2pConfig config = new WifiP2pConfig();
        config.deviceAddress = device.deviceAddress;
        config.wps.setup = WpsInfo.PBC;

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // BroadcastReceiver will automatically notify us
                Toast.makeText(P2PActivity.this,
                        "Connection success", Toast.LENGTH_SHORT).show();
            }
            @Override
            public void onFailure(int reasonCode) {
                // Try to create group for non wifi-direct device
                manager.createGroup(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        // Ready for connection
                        Log.i("Wifi Direct", "Group created successfully");
                    }
                    @Override
                    public void onFailure(int reasonCode) {
                        Toast.makeText(P2PActivity.this,
                                "Connection failed...", Toast.LENGTH_SHORT).show();
                    }
                });
//                Toast.makeText(P2PActivity.this,
//                        "Connection failed...", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        receiver = new BReceiver();
        registerReceiver(receiver, intentFilter);
    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }
}