package com.example.erezfri.pvd;
import android.app.AlertDialog;
import android.app.Service;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;

/**
 * Created by idanpor on 15/05/2015.
 */
public class GPSTracker extends Service implements LocationListener {
    private final Context context;

    boolean IsGpsEnable      = false;
    boolean CanGetLocation   = false;
    // boolean isNetworkEnabled = false;

    Location location;

    double  latitude, longtitude;

    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1;//meters accurate
    private static final long MIN_TIME_BW_FOR_UPDATES = 0;//update every minute

    protected LocationManager locationManager;



    public GPSTracker(Context context) {
        this.context = context;
        getLocation();
    }

    public Location getLocation()
    {
        try
        {
            locationManager  = (LocationManager) context.getSystemService(LOCATION_SERVICE);
            IsGpsEnable      = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            // isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            if ((!IsGpsEnable))// && (!isNetworkEnabled))
            {

            }
            else
            {
                this.CanGetLocation = true;
//                      if (isNetworkEnabled) {
//                          location=null;
//                          locationManager.requestLocationUpdates(locationManager.NETWORK_PROVIDER, MIN_TIME_BW_FOR_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
//
//                          if (locationManager != null) {
//                              location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
//
//                              if (location != null) {
//                                  latitude = location.getLatitude();
//                                  longtitude = location.getLongitude();
//                              }
//                          }
//                      }
                if (IsGpsEnable)
                {
                    location=null;
                    if (location == null)
                    {
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MIN_TIME_BW_FOR_UPDATES, MIN_DISTANCE_CHANGE_FOR_UPDATES, this);
                        if (locationManager != null)
                        {
                            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            if (location != null)
                            {
                                latitude   =  location.getLatitude();
                                longtitude =  location.getLongitude();
                            }
                        }
                    }
                }
            }


        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return location;
    }
    public void stopUsingGPS()
    {
        if (locationManager != null)
        {
            locationManager.removeUpdates(GPSTracker.this);
        }
    }

    public double getLatitude()
    {
        if (location != null)
        {
            latitude = location.getLatitude();
        }
        return latitude;
    }

    public double getLongitude()
    {
        if (location != null)
        {
            longtitude = location.getLongitude();
        }
        return longtitude;
    }

    public boolean canGetLocation()
    {
        return this.canGetLocation();
    }
    public void showSettingAlerts()
    {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);


        alertDialog.setTitle("GPS is setting");

        alertDialog.setMessage("GPS isn't enable. do you want to go to setting menu?");

        alertDialog.setPositiveButton("settings", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(intent);
            }
        });
        alertDialog.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        alertDialog.show();

    }
    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
