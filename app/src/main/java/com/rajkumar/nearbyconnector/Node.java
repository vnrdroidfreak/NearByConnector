package com.rajkumar.nearbyconnector;

import android.content.Context;

import com.virudhairaj.nearby.NBNode;

import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Node extends NBNode{
    public Node(@NonNull Context context, @NonNull String name, Type type) {
        super(context, name, type);
    }

    public Node(@NonNull String deviceId, @NonNull String name, Type type) {
        super(deviceId, name, type);
    }

    public Node(@NonNull JSONObject obj, @Nullable String endpointId) {
        super(obj, endpointId);
    }

    public Node(@NonNull String jsonObjStr, @Nullable String endpointId) {
        super(jsonObjStr, endpointId);
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
