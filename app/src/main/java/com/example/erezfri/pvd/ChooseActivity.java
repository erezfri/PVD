package com.example.erezfri.pvd;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
;

/**
 * Created by erezfri on 22/05/2015.
 */
public class ChooseActivity extends ActionBarActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choosing_screen);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
    //on monitor button click function
    public void onMonitorClick(View view){
        Intent intent = new Intent(this, MonitorActivity.class);
        startActivity(intent);
    }
    public void onSensorClick(View view){
        Intent intent = new Intent(this, SensorActivity.class);
        startActivity(intent);
    }
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit")
                .setMessage("Are you sure you want to exit?")
                .setNegativeButton(android.R.string.no, null)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface arg0, int arg1) {
                        ChooseActivity.super.onBackPressed();

                    }
                }).create().show();
    }
}
