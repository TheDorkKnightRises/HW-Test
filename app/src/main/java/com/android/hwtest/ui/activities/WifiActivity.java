package com.android.hwtest.ui.activities;

import android.Manifest;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.hwtest.R;

import java.util.List;

public class WifiActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    private static final int REQUEST_LOCATION_PERMISSION = 102;
    private WifiManager wifiManager;
    private Switch toggleSwitch;
    private RecyclerView recyclerView;
    private String[] wifis;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);

        toggleSwitch = findViewById(R.id.toggle_switch);
        toggleSwitch.setOnCheckedChangeListener(this);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        list = findViewById(R.id.list);
        list.setOnItemClickListener((parent, view, position, id) -> {
            // selected item
            String ssid = ((TextView) view).getText().toString();
            connectToWifi(ssid);
            Toast.makeText(WifiActivity.this, "Wifi SSID : " + ssid, Toast.LENGTH_SHORT).show();
        });


        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (wifiManager.isWifiEnabled()) {
            toggleSwitch.setChecked(true);
            toggleSwitch.setText(R.string.wifi_enabled);
            startDiscovery();
        } else {
            toggleSwitch.setChecked(false);
            toggleSwitch.setText(R.string.wifi_disabled);
        }
    }

    public void startDiscovery() {
        // TODO: Check if location is on, required on higher API levels
        Log.d(getLocalClassName(), "Starting discovery");
        wifiManager.startScan();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
                Log.d(getLocalClassName(), "Broadcast received");
                List<ScanResult> wifiScanList = wifiManager.getScanResults();
                wifis = new String[wifiScanList.size()];
                for (int i = 0; i < wifiScanList.size(); i++) {
                    wifis[i] = ((wifiScanList.get(i)).SSID);
                }
                Log.d(getLocalClassName(), wifis.length+"");
                list.setAdapter(new ArrayAdapter<String>(getApplicationContext(), R.layout.list_item_wifi, R.id.label, wifis));
            }

        }

    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Snackbar snackbar = Snackbar.make(recyclerView,
                    getString(R.string.location_permission_rationale), Snackbar.LENGTH_INDEFINITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                snackbar.setAction(R.string.grant, view -> requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION));
            }
            snackbar.show();
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (wifiManager.isWifiEnabled() != b) {
            if (b) {
                wifiManager.setWifiEnabled(true);
                toggleSwitch.setText(R.string.wifi_enabled);
                Toast.makeText(WifiActivity.this, R.string.wifi_enabled, Toast.LENGTH_SHORT).show();
                startDiscovery();
            } else {
                wifiManager.setWifiEnabled(false);
                toggleSwitch.setText(R.string.wifi_disabled);
                Toast.makeText(WifiActivity.this, R.string.wifi_disabled, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void finallyConnect(String networkPass, String networkSSID) {
        WifiConfiguration wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", networkSSID);
        wifiConfig.preSharedKey = String.format("\"%s\"", networkPass);

        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";
        conf.preSharedKey = "\"" + networkPass + "\"";
        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for (WifiConfiguration i : list) {
            if (i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
    }

    private void connectToWifi(final String wifiSSID) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.connect_dialog);
        dialog.setTitle("Connect to Network");
        TextView textSSID = (TextView) dialog.findViewById(R.id.textSSID1);

        Button dialogButton = (Button) dialog.findViewById(R.id.okButton);
        EditText pass = (EditText) dialog.findViewById(R.id.textPassword);
        textSSID.setText(wifiSSID);

        // if button is clicked, connect to the network;
        dialogButton.setOnClickListener(v -> {
            String password = pass.getText().toString();
            finallyConnect(password, wifiSSID);
            dialog.dismiss();
        });
        dialog.show();
    }

}