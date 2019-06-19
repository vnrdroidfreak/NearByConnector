package com.virudhairaj.nearby;

import android.support.annotation.NonNull;

import org.json.JSONObject;

public abstract class Message {
    public final String content;
    public final MsgType type;

    public Message(@NonNull String json) {
        String c = json;
        MsgType t = MsgType.unknown;
        try {
            JSONObject obj = new JSONObject(json);
            t = obj.has("type") ? MsgType.parse(obj.getString("type")) : MsgType.unknown;
            c = obj.has("content") ? obj.getString("content") : json;
        } catch (Exception e) {
//            e.printStackTrace();
        } finally {
            this.content = c;
            this.type = t;
        }
    }

    public Message(@NonNull String content, MsgType type) {
        this.content = content;
        this.type = type != null ? type : MsgType.unknown;
    }

    public JSONObject toJsonObj() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("content", content != null ? content : "");
            obj.put("type", type != null ? type.name() : "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return obj;
    }

    @Override
    public String toString() {
        return toJsonObj().toString();
    }
}
