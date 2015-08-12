package com.example.erezfri.pvd;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.util.Set;

public class SensorActivity extends ActionBarActivity {

    //timer
    private Handler mHandler = new Handler();
    private long startTime;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours,minutes,seconds,milliseconds;
    private long secs,mins,hrs,msecs;
    private boolean stopped = false;
    private Runnable startTimer;


    //bluetooth
    private BluetoothService mBTService = null;
    // Connection mechanism (side)
    private int mConnectSide = BluetoothService.SERVER;
    // Name of the connected device
    private String mConnectedDeviceName = null;

    private BluetoothAdapter mBluetoothAdapter = null;
    private static final int REQUEST_ENABLE_BT = 1;

    public SensorActivity() {
        startTimer = new Runnable() {
            public void run() {
                elapsedTime = System.currentTimeMillis() - startTime;
                updateTimer(elapsedTime);
                mHandler.postDelayed(this,REFRESH_RATE);
            }
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sensor_screen);
        Button stopButton = (Button)findViewById(R.id.stopButton);
        stopButton.setVisibility(View.GONE);

        //bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }
    }
    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }

        if (mBTService==null) {
            mBTService = new BluetoothService(this, bluetoothHandler, mConnectSide);
        }
        connectDevice();

        //Toast.makeText(getApplicationContext(), "Waiting for monitor connection", Toast.LENGTH_LONG).show();
        }

        public void startClick(View view){
            showStopButton();
            if(stopped){
                startTime = System.currentTimeMillis() - elapsedTime;
            }
            else{ startTime = System.currentTimeMillis();
            }
            mHandler.removeCallbacks(startTimer);
            mHandler.postDelayed(startTimer, 0);
            try{
                sendMessage("TAKEPICTURE");
            }
            catch (Exception e){}
        }

        public void stopClick(View view){
            hideStopButton();
            mHandler.removeCallbacks(startTimer);
            ((TextView)findViewById(R.id.counterText)).setText("00:00:00");
            stopped = false;
        }


        private void showStopButton(){
            (findViewById(R.id.startButton)).setVisibility(View.INVISIBLE);
            (findViewById(R.id.stopButton)).setVisibility(View.VISIBLE);
        }
        private void hideStopButton(){
            (findViewById(R.id.startButton)).setVisibility(View.VISIBLE);
            (findViewById(R.id.stopButton)).setVisibility(View.INVISIBLE);
        }

        private void updateTimer (float time){
            secs = (long)(time/1000);
            mins = (long)((time/1000)/60);
            hrs = (long)(((time/1000)/60)/60);
            /* Convert the seconds to String * and format to ensure it has * a leading zero when required */
            secs = secs % 60;
            seconds=String.valueOf(secs);
            if(secs == 0) {
                seconds = "00";
            }
            if(secs <10 && secs > 0){
                seconds = "0"+seconds;
            }
            /* Convert the minutes to String and format the String */
            mins = mins % 60;
            minutes=String.valueOf(mins);
            if(mins == 0){
                minutes = "00";
            }
            if(mins <10 && mins > 0){
                minutes = "0"+minutes;
            }
            /* Convert the hours to String and format the String */
            hours=String.valueOf(hrs);
            if(hrs == 0){
                hours = "00";
            }
            if(hrs <10 && hrs > 0){
                hours = "0"+hours;
            }
            /* Although we are not using milliseconds on the timer in this example * I included the code in the event that you wanted to include it on your own */
            milliseconds = String.valueOf((long)time);
            if(milliseconds.length()==2){
                milliseconds = "0"+milliseconds;
            }
            if(milliseconds.length()<=1){
                milliseconds = "00";
            }
           // milliseconds = milliseconds.substring(milliseconds.length()-3, milliseconds.length()-2);
             /* Setting the timer text to the elapsed time */
            ((TextView)findViewById(R.id.counterText)).setText(hours + ":" + minutes + ":" + seconds);
          //  ((TextView)findViewById(R.id.timerMs)).setText("." + milliseconds);
        }

        public void aboutClick(View view){
            new AlertDialog.Builder(this)
                    .setTitle("About Walking Pattern Detector")
                    .setMessage("This application is research designed to identify walking patterns .\n" +
                            "The app was developed by Technion STEM lab.")
                    .setPositiveButton(android.R.string.ok, null).create().show();
        }

    public void helpClick(View view) {
        new AlertDialog.Builder(this)
                .setTitle("Help")
                .setMessage("You are now in the sensor screen, please make sure you are connected to a monitor device")
                    .setPositiveButton(android.R.string.ok, null).create().show();

        }

    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, "Not Connected", Toast.LENGTH_SHORT).show();
            connectDevice();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mBTService.write(send);

        }
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler bluetoothHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_WRITE:
                    //byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    // String writeMessage = new String(writeBuf);
                    break;
                case BluetoothService.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //TextView view = (TextView) findViewById(R.id.multi_sensor_text_view);
                    //view.setText(readMessage);
                        Toast.makeText(getApplicationContext(), readMessage, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothService.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
        }

    };


    //bluetooth connection
    private void connectDevice() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                try {   //try to connect to one of the paired devices... problem if more than 1, should be able to choose from list
                    mBTService.connect(device);
                    if (mBTService.getState() == 3) //STATE_CONNECTED
                    {
                        break;
                    }
                }
                catch (Exception e){}
            }
        }
    }

}

