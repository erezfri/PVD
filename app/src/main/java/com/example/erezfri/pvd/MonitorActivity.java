package com.example.erezfri.pvd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Toast;

public class MonitorActivity extends ActionBarActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter myBluetoothAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.monitor_screen);

        // take an instance of BluetoothAdapter - Bluetooth radio
        myBluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        if (myBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(getApplicationContext(), "Device does not support Bluetooth",
                    Toast.LENGTH_SHORT).show();
        }
    }
      public void onSearchButtonClick(View view) {
        if (!myBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);

            Toast.makeText(getApplicationContext(), "Bluetooth turned on",
                    Toast.LENGTH_LONG).show();
        }
          Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
          startActivity(intent);
    }

    final BroadcastReceiver bReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            }
            //start new
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(bReceiver, filter);
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Toast.makeText(getApplicationContext(), "Bluetooth off",
                                Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Toast.makeText(getApplicationContext(), "Turning Bluetooth off...",
                                Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Toast.makeText(getApplicationContext(), "Bluetooth on",
                                Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Toast.makeText(getApplicationContext(), "Turning Bluetooth on...",
                                Toast.LENGTH_LONG).show();
                        break;
                }
            }
            //end new
        }
    };

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

}
