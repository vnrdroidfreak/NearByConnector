package com.virudhairaj.nearby;

import android.support.annotation.NonNull;

public interface NodeParser<NODE extends Node> {
    @NonNull
    public abstract NODE parseNode(@NonNull String endpointId, @NonNull String json);
}
