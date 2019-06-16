package com.virudhairaj.nearby;

import android.support.annotation.NonNull;

public enum MsgType {
    unknown, message, fileMeta;

    public static MsgType parse(@NonNull String type) {
        if (type.equalsIgnoreCase(message.toString())) {
            return message;
        } else if (type.equalsIgnoreCase(fileMeta.toString())) {
            return fileMeta;
        } else {
            return unknown;
        }
    }


    public boolean isUnknown() {
        return this == unknown;
    }

}
