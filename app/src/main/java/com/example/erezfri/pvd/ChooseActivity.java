package com.example.erezfri.pvd;

import android.content.Intent;
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
}
