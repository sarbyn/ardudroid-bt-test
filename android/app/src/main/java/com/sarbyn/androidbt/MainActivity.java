package com.sarbyn.androidbt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.util.List;
import java.util.Set;


public class MainActivity extends Activity {

    public static final String LOG_TAG = "ANDROID-BT";
    public static final String BLUETOOTH_DEVICE_NAME = "HC-06";

    BluetoothAdapter adapter = null;
    BluetoothDevice arduinoDevice = null;

    Button connectButton, disconnectButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = (Button) findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setupBTConnection();
                openBTConnection();
            }
        });

        disconnectButton = (Button) findViewById(R.id.button_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeBTConnection();
            }
        });


    }

    private void setupBTConnection() {
        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_unavailable), Toast.LENGTH_LONG).show();
            return;
        }

        if (!adapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
            return;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        for(BluetoothDevice device: pairedDevices) {
            Log.d(LOG_TAG, device.getName());
            if (device.getName().equals(BLUETOOTH_DEVICE_NAME)) {
                arduinoDevice = device;
            }
        }

        if (arduinoDevice == null) {
            Toast.makeText(this, getString(R.string.no_arduino_device_found), Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, getString(R.string.arduino_device_found), Toast.LENGTH_LONG).show();
        }
    }

    private void openBTConnection() {

    }

    private void closeBTConnection() {
        Toast.makeText(this, getString(R.string.bluetooth_disconnected), Toast.LENGTH_LONG).show();
    }
}
