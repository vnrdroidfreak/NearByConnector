package com.virudhairaj.nearby;

import android.content.Context;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.json.JSONObject;

public class Node {
    public final String deviceId;
    public final String name;
    private String endpointId;
    public final Type type;
    private boolean connected;


    public static enum Type {
        generic, master, slave;

        public static Type parse(@NonNull String type) {
            if (type.equalsIgnoreCase(master.toString())) {
                return Type.master;
            } else if (type.equalsIgnoreCase(slave.toString())) {
                return Type.slave;
            } else {
                return Type.generic;
            }
        }
    }
    public Node(@NonNull Context context, @NonNull String name, Type type) {
        deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        this.name = name;
        this.type = type != null ? type : Type.generic;
    }

    public Node(@NonNull String deviceId, @NonNull String name, Type type) {
        this.deviceId = deviceId;
        this.name = name;
        this.type = type;
    }

    public Node(@NonNull JSONObject obj, @Nullable String endpointId) {
        this(obj.toString(),endpointId);
    }

    public Node(@NonNull String jsonObjStr, @Nullable String endpointId){
        String id = "", name = "";
        Type type = Type.generic;
        try {
            JSONObject obj=new JSONObject(jsonObjStr);
            id = obj.has("deviceId") ? obj.getString("deviceId") : "";
            name = obj.has("name") ? obj.getString("name") : "";
            type = Type.parse(obj.has("type") ? obj.getString("type") : Type.generic.name());
            if (obj.has("endpointId")) {
                this.endpointId = obj.getString("endpointId");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.deviceId = id;
            this.name = name;
            this.type = type;
        }

        if (TextUtils.isEmpty(this.endpointId)  && !TextUtils.isEmpty(endpointId)){
            this.endpointId=endpointId;
        }
    }

    public String getEndpointId() {
        return endpointId != null ? endpointId : "";
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public Node setConnected(boolean connected) {
        this.connected = connected;
        return this;
    }

    public boolean isConnected() {
        return connected;
    }

    JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("deviceId", deviceId);
            obj.put("name", name);
            if (!TextUtils.isEmpty(endpointId))
                obj.put("endpointId", endpointId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }


    @Override
    public String toString() {
        return toJsonObject().toString();
    }

//    public String BitMapToString(Bitmap bitmap){
//        ByteArrayOutputStream baos=new  ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.PNG,100, baos);
//        byte [] b=baos.toByteArray();
//        String temp= Base64.encodeToString(b, Base64.DEFAULT);
//        return temp;
//    }
}
