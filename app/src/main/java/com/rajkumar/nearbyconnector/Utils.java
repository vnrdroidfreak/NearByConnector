package com.rajkumar.nearbyconnector;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
}
