package com.virudhairaj.nearby;

import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;

import java.io.File;

public class NBCallback<NODE extends Node> {
    public void onStopDiscovery(@NonNull NBConnector<NODE> connector) {
    }

    public void onStopAdvertising(@NonNull NBConnector<NODE> connector) {
    }

    public void onAdvertiseCanceled(@NonNull NBConnector<NODE> connector) {
    }

    public void onAdvertiseSuccess(@NonNull NBConnector<NODE> connector) {
    }

    public void onAdvertiseFailed(@NonNull NBConnector<NODE> connector, final @NonNull Exception e) {
    }

    public void onDiscoveryCanceled(@NonNull NBConnector<NODE> connector) {
    }

    public void onDiscoverySuccess(@NonNull NBConnector<NODE> connector) {
    }

    public void onDiscoveryFailed(@NonNull NBConnector<NODE> connector, final @NonNull Exception e) {
    }

    public void onConnectionInitiated(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull ConnectionInfo connectionInfo) {
    }

    public void onConnectionSuccess(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull ConnectionResolution result) {
    }

    public void onConnectionRejected(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull ConnectionResolution result) {
    }

    public void onConnectionFailed(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull ConnectionResolution result) {
    }

    public void onDisconnected(@NonNull NBConnector<NODE> connector, final @NonNull NODE node) {
    }

    public void onNodeFound(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull DiscoveredEndpointInfo info) {
    }

    public void onNodeLost(@NonNull NBConnector<NODE> connector, final @NonNull NODE node) {
    }

    public void onMessageReceived(@NonNull NBConnector<NODE> connector, final @NonNull NODE from, final @NonNull Message message) {
    }

    public void onIncomingFile(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransfer(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, boolean isIncoming, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransferCanceled(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, boolean isIncoming, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransferFailed(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, boolean isIncoming, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileReceived(@NonNull NBConnector<NODE> connector, final @NonNull NODE node, final @NonNull File file) {
    }

}
