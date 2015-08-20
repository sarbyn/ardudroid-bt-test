package com.sarbyn.androidbt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;


public class MainActivity extends Activity {

    public static final String LOG_TAG = "ANDROID-BT";
    //    public static final String BLUETOOTH_DEVICE_NAME = "HC-06";
    public static final String SERIAL_UUID = "00001101-0000-1000-8000-00805F9B34FB";

    static final int SELECT_DEVICE_REQUEST = 1;

    String bluetoothDeviceName = null;

    BluetoothAdapter adapter = null;
    BluetoothDevice arduinoDevice = null;
    BluetoothSocket bluetoothSocket = null;

    InputStream is;
    OutputStream os;

    Button connectButton, disconnectButton, changeDeviceButton;
    Switch ledSwitch;
    TextView connectionInfo, outputText;

    boolean threadIsRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        connectButton = (Button) findViewById(R.id.button_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (setupBTConnection()) {
                    openBTConnection();
                }
            }
        });

        disconnectButton = (Button) findViewById(R.id.button_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeBTConnection();
            }
        });

        changeDeviceButton = (Button) findViewById(R.id.change_device_button);
        changeDeviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bluetoothDeviceName = null;
                setupBTConnection();
            }
        });

        ledSwitch = (Switch) findViewById(R.id.led_switch);
        ledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changeLedStatus(isChecked);
            }
        });

        connectionInfo = (TextView) findViewById(R.id.connectionInfo);
        outputText = (TextView) findViewById(R.id.output_text);
    }

    private boolean setupBTConnection() {
        adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_unavailable), Toast.LENGTH_LONG).show();
            return false;
        }

        if (!adapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
            return false;
        }

        if (bluetoothDeviceName == null) {
            Intent i = new Intent(MainActivity.this, SetupBTDeviceActivity.class);
            MainActivity.this.startActivityForResult(i, SELECT_DEVICE_REQUEST);
            return false;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        for(BluetoothDevice device: pairedDevices) {
            Log.d(LOG_TAG, device.getName());
            if (device.getName().equals(bluetoothDeviceName)) {
                arduinoDevice = device;
            }
        }

        connectionInfo.setText(getString(R.string.connecting_to) + arduinoDevice.getName());

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SELECT_DEVICE_REQUEST) {
            if (resultCode == RESULT_OK) {
                bluetoothDeviceName = data.getStringExtra(SetupBTDeviceActivity.DEVICE_NAME_EXTRA);
                setupBTConnection();
                openBTConnection();
            }
        }
    }

    private void openBTConnection() {
        try {
            if (arduinoDevice == null) {
                throw new IOException("No arduino device detected");
            }

            if (threadIsRunning) {
                Log.d(LOG_TAG, "Thread already running, skip open connection");
                return;
            }

            UUID uuid = UUID.fromString(SERIAL_UUID);
            bluetoothSocket = arduinoDevice.createRfcommSocketToServiceRecord(uuid);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        bluetoothSocket.connect();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectionInfo.setText(getString(R.string.connected_to) + arduinoDevice.getName());
                            }
                        });

                        /* TODO timeout */
                        os = bluetoothSocket.getOutputStream();
                        is = bluetoothSocket.getInputStream();

                        startSocketThread();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Unable to open socket", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                connectionInfo.setText(e.getMessage());
                                Toast.makeText(MainActivity.this, getString(R.string.unable_to_open_bt_socket), Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }
            }).start();

        } catch (IOException e) {
            Log.e(LOG_TAG, "Unable to open socket", e);
            connectionInfo.setText(e.getMessage());
            Toast.makeText(MainActivity.this, getString(R.string.unable_to_open_bt_socket), Toast.LENGTH_LONG).show();
        }
    }

    private void closeBTConnection() {
        Toast.makeText(this, getString(R.string.bluetooth_disconnected), Toast.LENGTH_LONG).show();
        connectionInfo.setText(getString(R.string.disconnected));
        threadIsRunning = false;
        try {
            if (is != null) is.close();
            if (os != null) os.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void changeLedStatus(boolean isOn) {
        try {
            os.write(isOn ? (byte) 1 : (byte) 0);
            os.flush();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Unable to send LED command", e);
            Toast.makeText(this, getString(R.string.unable_to_send_command_to_device), Toast.LENGTH_LONG).show();
            ledSwitch.setChecked(!isOn);
            closeBTConnection();
        }
    }

    private void showMessageFromArduino(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                outputText.setText(message);
            }
        });
    }

    private void startSocketThread() {
        threadIsRunning = true;

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[20];
                int bufferPosition = 0;
                final byte END_LINE = (byte) '\n';

                while (threadIsRunning) {
                    try {
                        int bytesAvailable = is.available();
                        if (bytesAvailable > 0) {
                            byte[] readBuffer = new byte[bytesAvailable];
                            is.read(readBuffer);

                            for (int i = 0; i < bytesAvailable; i++) {
                                byte b = readBuffer[i];
                                if(END_LINE != b) {
                                    buffer[bufferPosition] = b;
                                    bufferPosition++;
                                } else {
                                    byte[] messageFromArduino = new byte[bufferPosition];
                                    System.arraycopy(buffer, 0, messageFromArduino, 0, bufferPosition);
                                    Log.d(LOG_TAG, "Message " + new String(messageFromArduino));
                                    showMessageFromArduino(new String(messageFromArduino));
                                    bufferPosition = 0;
                                    Arrays.fill(buffer, (byte) 0);
                                }
                            }
                        }
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Exception in connectionThread", e);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                closeBTConnection();
                            }
                        });
                    }
                }
            }
        }, "READ-THREAD").start();
    }
}
