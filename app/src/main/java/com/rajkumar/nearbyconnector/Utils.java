package com.rajkumar.nearbyconnector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class Utils {

    public static void addWifi(final @NonNull Context context, final String ssid, final String password) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "permission missing " + Manifest.permission.ACCESS_WIFI_STATE);
            return;
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CHANGE_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            Log.e("Permission", "permission missing " + Manifest.permission.CHANGE_WIFI_STATE);
            return;
        }

        final WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }

        WifiConfiguration config = new WifiConfiguration();
        config.SSID = ssid;
        config.preSharedKey = password;
        int netId = wifiManager.addNetwork(config);
        wifiManager.disconnect();
        wifiManager.enableNetwork(netId, true);
        wifiManager.reconnect();
    }


//    public static void updateTime(final @NonNull Context context, final String ssid, final String password) {
//        if (ContextCompat.checkSelfPermission(context, "com.google.android.things.permission.SET_TIME") != PackageManager.PERMISSION_GRANTED) {
//
//            Log.e("Permission", "permission missing com.google.android.things.permission.SET_TIME");
//            return;
//        }
//
//        TimeManager timeManager = TimeManager.getInstance();
//        // Use 24-hour time
//        timeManager.setTimeFormat(TimeManager.FORMAT_24);
//
//        // Set time zone to Eastern Standard Time
//        timeManager.setTimeZone("America/New_York");
//
//        // Set clock time to noon
//        Calendar calendar = Calendar.getInstance();
//        calendar.set(Calendar.MILLISECOND, 0);
//        calendar.set(Calendar.SECOND, 0);
//        calendar.set(Calendar.MINUTE, 0);
//        calendar.set(Calendar.HOUR_OF_DAY, 12);
//        long timeStamp = calendar.getTimeInMillis();
//        timeManager.setTime(timeStamp);
//    }
}
