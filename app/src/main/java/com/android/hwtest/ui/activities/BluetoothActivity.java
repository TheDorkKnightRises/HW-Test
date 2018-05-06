package com.android.hwtest.ui.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.android.hwtest.R;
import com.android.hwtest.ui.adapters.BluetoothDeviceAdapter;

import java.util.ArrayList;
import java.util.List;

public class BluetoothActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private static final int REQUEST_ENABLE_BT = 101;
    private static final int REQUEST_LOCATION_PERMISSION = 102;
    private static final int REQUEST_STORAGE_PERMISSION = 103;
    private static final int REQUEST_PICK_FILE = 104;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDeviceAdapter mAdapter;
    private Switch toggleSwitch;
    private RecyclerView recyclerView;
    private Button sendButton;
    private ArrayList<com.android.hwtest.models.BluetoothDevice> devices;
    private ArrayList<String> files;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        toggleSwitch = findViewById(R.id.toggle_switch);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bt_not_supported, Toast.LENGTH_SHORT).show();
            return;
        }

        toggleSwitch.setOnCheckedChangeListener(this);

        devices = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new BluetoothDeviceAdapter(this, devices);
        recyclerView.setAdapter(mAdapter);

        sendButton = findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                        && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_STORAGE_PERMISSION);
                } else {
                    showFilePicker();
                }
            }
        });

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mBluetoothAdapter.isEnabled()) {
            toggleSwitch.setChecked(true);
            toggleSwitch.setText(R.string.bluetooth_enabled);
            startDiscovery();
        } else {
            toggleSwitch.setChecked(false);
            toggleSwitch.setText(R.string.bluetooth_disabled);
            devices.clear();
            mAdapter.notifyDataSetChanged();
            sendButton.setEnabled(false);
        }
    }

    public void startDiscovery() {
        Log.d(getLocalClassName(), "Starting discovery");
        if (mBluetoothAdapter.isDiscovering())
            mBluetoothAdapter.cancelDiscovery();
        devices.clear();
        mAdapter.notifyDataSetChanged();
        mBluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d(getLocalClassName(), "Device found: " + deviceName + " (" + deviceHardwareAddress + ")");
                devices.add(new com.android.hwtest.models.BluetoothDevice(deviceName, deviceHardwareAddress));
                mAdapter.notifyDataSetChanged();
                sendButton.setEnabled(true);
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }

        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                toggleSwitch.setText(R.string.bluetooth_enabled);
                Toast.makeText(BluetoothActivity.this, R.string.bluetooth_enabled, Toast.LENGTH_SHORT).show();
                devices.clear();
                mAdapter.notifyDataSetChanged();
                startDiscovery();
            } else {
                toggleSwitch.setOnCheckedChangeListener(null);
                toggleSwitch.setChecked(false);
                toggleSwitch.setOnCheckedChangeListener(this);
                Toast.makeText(BluetoothActivity.this, R.string.bluetooth_not_enabled, Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_PICK_FILE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                //files = new ArrayList<>(data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS));
                //File file = new File(files.get(0));
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("*/*");
                Uri uri = null;
                uri = data.getData();
                Log.i(getLocalClassName(), "Uri: " + uri.toString());
                shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                /*shareIntent.putExtra(Intent.EXTRA_STREAM,
                        FileProvider.getUriForFile(BluetoothActivity.this,
                                BuildConfig.APPLICATION_ID + ".provider", file));*/
                PackageManager pm = getPackageManager();
                List<ResolveInfo> list =
                        pm.queryIntentActivities(shareIntent, 0);
                if (list.size() > 0) {
                    String packageName = null;
                    String className = null;
                    boolean found = false;
                    for (ResolveInfo info : list){
                        packageName = info.activityInfo.packageName;
                        if (packageName.equals("com.android.bluetooth")) {
                            className = info.activityInfo.name;
                            found = true;
                            break;
                        }
                    }

                    if (!found) {
                        Toast.makeText(this, "Bluetooth not found", Toast.LENGTH_SHORT).show();
                    } else {
                        shareIntent.setClassName(packageName, className);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(shareIntent);
                    }
                }
            } else {
                Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Snackbar snackbar = Snackbar.make(recyclerView,
                    getString(R.string.location_permission_rationale), Snackbar.LENGTH_INDEFINITE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                snackbar.setAction(R.string.grant, new View.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onClick(View view) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
                    }
                });
            }
            snackbar.show();
        } else if (requestCode == REQUEST_STORAGE_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Snackbar snackbar = Snackbar.make(recyclerView,
                        getString(R.string.location_permission_rationale), Snackbar.LENGTH_INDEFINITE);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    snackbar.setAction(R.string.grant, new View.OnClickListener() {
                        @TargetApi(Build.VERSION_CODES.M)
                        @Override
                        public void onClick(View view) {
                            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE_PERMISSION);
                        }
                    });
                }
                snackbar.show();
            } else {
                showFilePicker();
            }
        }
    }

    private void showFilePicker() {
        /*FilePickerBuilder.getInstance().setMaxCount(5)
                .setMaxCount(1)
                .setActivityTheme(R.style.AppTheme_NoActionBar)
                .pickFile(this, REQUEST_PICK_FILE);*/
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");

        startActivityForResult(intent, REQUEST_PICK_FILE);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (mBluetoothAdapter.isEnabled() != b) {
            if (b) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            } else {
                mBluetoothAdapter.cancelDiscovery();
                mBluetoothAdapter.disable();
                sendButton.setEnabled(false);
                toggleSwitch.setText(R.string.bluetooth_disabled);
                devices.clear();
                mAdapter.notifyDataSetChanged();
                Toast.makeText(BluetoothActivity.this, R.string.bluetooth_disabled, Toast.LENGTH_SHORT).show();
            }
        }
    }


}
