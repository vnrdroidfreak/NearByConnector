package com.virudhairaj.nearby;

import android.support.annotation.NonNull;



import java.io.File;
import java.io.FileNotFoundException;

public class MsgFileMeta extends Message {

    private MsgFileMeta(@NonNull String json) {
        super(json);
    }

    private MsgFileMeta(@NonNull String content, MsgType type) {
        super(content, type);
    }

    public static MsgFileMeta fromFile(File file) throws FileNotFoundException {
        if (!file.exists()) throw new FileNotFoundException(file.getAbsolutePath());
        int index = file.getAbsolutePath().lastIndexOf(File.separator);
        String fileName = file.getAbsolutePath().substring(index + 1);
        return new MsgFileMeta(fileName,MsgType.fileMeta);
    }

    public static MsgFileMeta fromJsonStr(@NonNull String json){
        return new MsgFileMeta(json);
    }

    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }
}
