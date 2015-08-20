package com.sarbyn.androidbt;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SetupBTDeviceActivity extends Activity {

    public static final String DEVICE_NAME_EXTRA = "DEVICE_NAME";
    ListView deviceList;
    ArrayList<String> devicesByName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_btdevice);

        devicesByName = new ArrayList<String>();

        deviceList = (ListView) findViewById(R.id.bt_list_view);
        populateView();

        deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent result = new Intent();
                result.putExtra(DEVICE_NAME_EXTRA, devicesByName.get(position));
                setResult(RESULT_OK, result);
                finish();
            }
        });
    }


    private void populateView() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter == null) {
            Toast.makeText(this, getString(R.string.bluetooth_unavailable), Toast.LENGTH_LONG).show();
            return;
        }

        if (!adapter.isEnabled()) {
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), 0);
            return;
        }

        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        devicesByName.clear();
        for (BluetoothDevice device : pairedDevices) {
            devicesByName.add(device.getName());
        }

        DeviceArrayAdapter listAdapter = new DeviceArrayAdapter(this, android.R.layout.simple_list_item_1, devicesByName);
        deviceList.setAdapter(listAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setup_bt_device_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.refresh:
                populateView();
                return true;
        }

        return false;
    }

    private class DeviceArrayAdapter extends ArrayAdapter<String> {
        HashMap<String, Integer> mIdMap = new HashMap<String, Integer>();

        public DeviceArrayAdapter(Context context, int textViewResourceId,
                                  List<String> objects) {
            super(context, textViewResourceId, objects);
            for (int i = 0; i < objects.size(); ++i) {
                mIdMap.put(objects.get(i), i);
            }
        }

        @Override
        public long getItemId(int position) {
            String item = getItem(position);
            return mIdMap.get(item);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

    }
}
