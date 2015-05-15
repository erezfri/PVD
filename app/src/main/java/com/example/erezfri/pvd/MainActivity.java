package com.example.erezfri.pvd;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
        builder1.setTitle("Welcome to the PVD Detector app");
        builder1.setMessage(
                "This app will help you to determine if you are at risk for \n" +
                        "the Peripheral Vascular Disease (PVD). For best results you " +
                        "should perform the test at least 3 times in 3 different days.\n" +
                        "Each test takes 15 minutes and you can stop it \n" +
                        "in the middle while the results will not be saved. \n" +
                        "In the end of the test we will show you a report of \n" +
                        "the current test and how many more test you need to \n" +
                        "perform. After you are done with the 3 tests we will \n" +
                        "analyze the data and we will show you a conclusion.");

        builder1.setCancelable(true);
        builder1.setPositiveButton("Got it, let's start",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });


        AlertDialog alert11 = builder1.create();
        alert11.show();


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
