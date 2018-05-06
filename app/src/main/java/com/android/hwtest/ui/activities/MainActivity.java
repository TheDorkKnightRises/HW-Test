package com.android.hwtest.ui.activities;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.android.hwtest.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.wifi_view).setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, WifiActivity.class)));
        findViewById(R.id.bt_view).setOnClickListener(
                v -> startActivity(new Intent(MainActivity.this, BluetoothActivity.class)));
    }
}
