package com.example.janus_android_jw.gps;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

public class GPSControll {
    private LocationManager mLocationManager;
    private String bestProvider = null;
    private long updateRate = 10 * 1000;
    public static final byte[] UPDATE_MODE = { 0x00, 0x01, 0x02, 0x03, 0x04,
            0x05, 0x06, 0x07, 0x08, 0x09 };
    public static final byte[] UPDATE_REALTIME = { 0x00, 0x01 };
    private GPSInfo mCurrentInfo = null;
    private int mGpsState = 0;
    private Context mContext;

    GPSControll(Context context){
        mContext = context;
        mLocationManager = (LocationManager)mContext.getSystemService(Context.LOCATION_SERVICE);
        bestProvider = mLocationManager.getBestProvider(getCriteria(), true);
    }

    private Criteria getCriteria() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);
        criteria.setSpeedRequired(true);
        criteria.setCostAllowed(false);
        criteria.setBearingRequired(true);
        criteria.setAltitudeRequired(true);
        criteria.setPowerRequirement(Criteria.POWER_LOW);
        return criteria;
    }

    public void startLocation() {
        if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationManager.requestLocationUpdates(bestProvider, updateRate, 0,
                    locationListener);
        }
    }

    public GPSInfo getCurrentLocationInfo(){
        return mCurrentInfo;
    }

    private LocationListener locationListener = new LocationListener() {

        @Override
        public void onStatusChanged(String provider, int status, Bundle extra) {
        }

        @Override
        public void onProviderEnabled(String provider) {
            if (ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Location location = mLocationManager
                            .getLastKnownLocation(bestProvider);
                    mCurrentInfo = getLocationInfo(location);
                }
                //mLastInfo = mCurrentInfo;
            }
        }
        @Override
        public void onProviderDisabled(String provider) {
            mGpsState = 0;
        }

        @Override
        public void onLocationChanged(Location location) {
            mCurrentInfo = getLocationInfo(location);
            mGpsState = 1;
        }
    };

    private GPSInfo getLocationInfo(Location location) {
        if (location == null) {
            return null;
        }
        GPSInfo info = new GPSInfo();
        info.setTime(location.getTime()/1000);
        info.setLatitude(location.getLatitude());
        info.setLongitude(location.getLongitude());
        info.setSpeed(location.getSpeed());
        info.setBearing(location.getBearing());
        info.setSupply(UPDATE_REALTIME[0]);
        info.setMode(UPDATE_MODE[0]);
        return info;
    }
}
