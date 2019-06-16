package com.virudhairaj.nearby;

import android.os.ParcelFileDescriptor;

import com.google.android.gms.nearby.connection.Payload;

import java.io.File;
import java.io.FileNotFoundException;

public class FilePayload {
    private final File inputFile;

    public FilePayload(File inputFile) {
        this.inputFile = inputFile;
    }



    public Payload toMsgFileMeta()throws FileNotFoundException{
        return Payload.fromBytes(MsgFileMeta.fromFile(inputFile).toString().getBytes());
    }

    public Payload toFilePayload() throws FileNotFoundException {
        return Payload.fromFile(ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_APPEND));
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
