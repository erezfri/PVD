package com.example.erezfri.pvd;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.UUID;

public class MonitorActivity extends ActionBarActivity {

    private static final String UUID_SERIAL_PORT_PROFILE
            = "00001101-0000-1000-8000-00805F9B34FB";
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter myBluetoothAdapter;
    private BluetoothSocket mSocket = null;
    private BufferedReader mBufferedReader = null;

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
                try{//new for read data from BT
                    openDeviceConnection(device);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //end  for read data from BT

            }
            //start new code for changing the BT status
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

    //new code for data reading
    private void openDeviceConnection(BluetoothDevice aDevice)
            throws IOException {
        InputStream aStream = null;
        InputStreamReader aReader = null;
        try {
            mSocket = aDevice
                    .createRfcommSocketToServiceRecord( getSerialPortUUID() );
            mSocket.connect();
            aStream = mSocket.getInputStream();
            aReader = new InputStreamReader( aStream );
            mBufferedReader = new BufferedReader( aReader );
        } catch ( IOException e ) {
            //"Could not connect to device"
            close( mBufferedReader );
            close( aReader );
            close( aStream );
            close( mSocket );
            throw e;
        }
    }

    private void close(Closeable aConnectedObject) {
        if ( aConnectedObject == null ) return;
        try {
            aConnectedObject.close();
        } catch ( IOException e ) {
        }
        aConnectedObject = null;
    }

    private UUID getSerialPortUUID() {
        return UUID.fromString( UUID_SERIAL_PORT_PROFILE );
    }
    //end new code for data reading

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(bReceiver);
    }

}
