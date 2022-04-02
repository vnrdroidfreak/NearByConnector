package com.virudhairaj.nearby;


import androidx.annotation.NonNull;

public interface NBNodeParser<NODE extends NBNode> {
    @NonNull
    public abstract NODE parseNode(@NonNull String endpointId, @NonNull String json);
}
