package com.virudhairaj.nearby;

import android.support.annotation.NonNull;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Payload;

import java.io.File;

public abstract class NBCallback<NODE extends Node> {

    public void onStopDiscovery() {
    }

    public void onStopAdvertising() {
    }

    public void onAdvertiseCanceled() {
    }

    public void onAdvertiseComplete() {
    }

    public void onAdvertiseSuccess() {
    }

    public void onAdvertiseFailed(final @NonNull Exception e) {
    }

    public void onDiscoveryCanceled() {
    }

    public void onDiscoverySuccess() {
    }

    public void onDiscoveryComplete() {
    }

    public void onDiscoveryFailed(final @NonNull Exception e) {
    }

    public void onConnectionInitiated(final @NonNull NODE node, final @NonNull ConnectionInfo connectionInfo) {
    }

    public void onConnectionSuccess(final @NonNull NODE node, final @NonNull ConnectionResolution result) {
    }

    public void onConnectionRejected(final @NonNull NODE node, final @NonNull ConnectionResolution result) {
    }

    public void onConnectionFailed(final @NonNull NODE node, final @NonNull ConnectionResolution result) {
    }

    public void onDisconnected(final @NonNull NODE node) {
    }

    public void onDeviceFound(final @NonNull NODE node, final @NonNull DiscoveredEndpointInfo info) {
    }

    public void onDeviceLost(final @NonNull NODE node) {
    }

    public void onMessageReceived(final @NonNull NODE from, final @NonNull String message) {
    }

    public void onIncomingFile(final @NonNull NODE from, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransfer(final @NonNull NODE from, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransferCanceled(final @NonNull NODE from, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransferFailed(final @NonNull NODE from, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileReceived(final @NonNull NODE from, final @NonNull File file) {
    }

}
