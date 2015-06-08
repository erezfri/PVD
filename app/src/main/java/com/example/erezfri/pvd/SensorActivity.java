package com.example.erezfri.pvd;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SensorActivity extends ActionBarActivity {

    private Handler mHandler = new Handler();
    private long startTime;
    private long elapsedTime;
    private final int REFRESH_RATE = 100;
    private String hours,minutes,seconds,milliseconds;
    private long secs,mins,hrs,msecs;
    private boolean stopped = false;
    private Runnable startTimer;

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
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent turnOnIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(turnOnIntent, REQUEST_ENABLE_BT);
            Intent intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(intent);
        }

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

}

