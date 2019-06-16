package com.virudhairaj.nearby;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.SimpleArrayMap;
import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsClient;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public abstract class NBConnector<NODE extends Node> {
    private static final String TAG = NBConnector.class.getSimpleName();
    private final ConnectionsClient connection;
    private final NODE node;
    private NBCallback<NODE> callback;
    public static boolean debuggable = true;

    private final SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> completedFilePayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> filePayloadFilenames = new SimpleArrayMap<>();

    /**
     * code for nodes management
     */
    private final HashMap<String, NODE> connected;
    private final HashMap<String, NODE> nearBy;

    private void putDeviceNearBy(@NonNull NODE node) {
        try {
            connected.remove(node.getEndpointId()); //first remove from connected list if exist
            nearBy.put(node.getEndpointId(), node);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void putDeviceOnline(@NonNull NODE node) {
        try {
            nearBy.remove(node.getEndpointId());
            connected.put(node.getEndpointId(), node);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void putDeviceOnline(@NonNull String endpointId) {
        try {
            final NODE node = nearBy.remove(endpointId);
            if (node != null)
                connected.put(endpointId, node);
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private NODE removeDevice(@NonNull String endpointId) {
        NODE c = connected.remove(endpointId);
        NODE n = nearBy.remove(endpointId);
        return c != null ? c : n;
    }

    private NODE removeDevice(@NonNull NODE node) {
        return removeDevice(node.getEndpointId());
    }

    private NODE removeNearByDevice(@NonNull String endpointId) {
        return nearBy.remove(endpointId);
    }

    private NODE removeConnectedDevice(@NonNull String deviceId) {
        return connected.remove(deviceId);
    }

    private void clearDevices() {
        connected.clear();
        nearBy.clear();
    }


    private NODE findOnlineDeviceById(@NonNull String endpointId) {
        return connected.get(endpointId);
    }

    private NODE findAvailableDeviceById(@NonNull String endpointId) {
        return nearBy.get(endpointId);
    }

    @NonNull
    public abstract NODE parseNode(@NonNull String endpointId, @NonNull String json);

    public NBConnector(@NonNull final ConnectionsClient connection, @NonNull final NODE myNode) {
        this.connection = connection;
        this.node = myNode;
        connected = new HashMap<>();
        nearBy = new HashMap<>();
    }

    public NBConnector(@NonNull final Context context, @NonNull final NODE myNode) {
        if (context == null) throw new RuntimeException("Context is null");
        if (myNode == null) throw new RuntimeException("myNode is null");
        this.connection = Nearby.getConnectionsClient(context);
        this.node = myNode;
        connected = new HashMap<>();
        nearBy = new HashMap<>();
    }

    public NBConnector setCallback(@Nullable final NBCallback<NODE> callback) {
        this.callback = callback;
        return this;
    }

    private void processFilePayload(long payloadId) {
        // BYTES and FILE could be received in any order, so we call when either the BYTES or the FILE
        // payload is completely received. The file payload is considered complete only when both have
        // been received.
        Payload filePayload = completedFilePayloads.get(payloadId);
        String filename = filePayloadFilenames.get(payloadId);
        if (filePayload != null && filename != null) {
            completedFilePayloads.remove(payloadId);
            filePayloadFilenames.remove(payloadId);

            // Get the received file (which will be in the Downloads folder)
            File payloadFile = filePayload.asFile().asJavaFile();

            // Rename the file.
            payloadFile.renameTo(new File(payloadFile.getParentFile(), filename));
        }
    }

    private long addPayloadFilename(String payloadFilenameMessage) {
        String[] parts = payloadFilenameMessage.split(":");
        long payloadId = Long.parseLong(parts[0]);
        String filename = parts[1];
        filePayloadFilenames.put(payloadId, filename);
        return payloadId;
    }

    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    final NODE node = connected.get(endpointId);
                    Log.e(TAG, "onPayloadReceived from " + node);
                    incomingPayloads.put(payload.getId(), payload);

                    final String content = new String(payload.asBytes(), StandardCharsets.UTF_8);
                    if (payload.getType() == Payload.Type.BYTES && content.contains(MsgType.fileMeta.name())) {
                        MsgFileMeta meta = MsgFileMeta.fromJsonStr(content);
                        Log.e(TAG, "onIncomingFile: from " + node + " fileName: " + meta.content);
                        if (callback != null && node != null)
                            callback.onIncomingFile(node, payload, meta.content);
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    final Payload payload = incomingPayloads.get(update.getPayloadId());
                    if (payload == null) {
                        Log.e(TAG, "onPayloadTransferUpdate: payload not found ");
                        return;
                    }
                    final NODE node = connected.get(endpointId);

                    if (node == null) {
                        Log.e(TAG, "onPayloadTransferUpdate: endpoint not found in connected list");
                        return;
                    }

                    if (payload.getType() == Payload.Type.BYTES && update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                        final String content = new String(payload.asBytes(), StandardCharsets.UTF_8);
                        incomingPayloads.remove(payload);
                        if (content.contains(MsgType.fileMeta.name())) {
                            Log.e(TAG, "onFileMetaReceived: from : " + node + " content: " + content);
                            MsgFileMeta meta = MsgFileMeta.fromJsonStr(content);
                            filePayloadFilenames.put(payload.getId(), meta.content);
                        } else {
                            Log.e(TAG, "onMessageReceived: from : " + node + " content: " + content);
                            if (callback != null) callback.onMessageReceived(node, content);

                        }
                    } else if (payload.getType() == Payload.Type.FILE) {
                        if (update.getStatus() == PayloadTransferUpdate.Status.IN_PROGRESS) {
                            if (callback != null)
                                callback.onFileTransfer(node, payload, filePayloadFilenames.get(payload.getId()));
                        } else if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                            final Payload completed = incomingPayloads.remove(payload.getId());
                            filePayloadFilenames.remove(payload.getId());
                            if (callback != null)
                                callback.onFileReceived(node, payload.asFile().asJavaFile());
                        } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                            final Payload canceled = incomingPayloads.remove(payload.getId());
                            final String fileName = filePayloadFilenames.remove(payload.getId());
                            if (callback != null && canceled!=null)
                                callback.onFileTransferCanceled(node, canceled, fileName);
                        } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                            incomingPayloads.remove(payload.getId());
                            final String fileName = filePayloadFilenames.remove(payload.getId());
                            if (callback != null )
                                callback.onFileTransferFailed(node, payload, fileName);
                        }
                    } else if (payload.getType() == Payload.Type.STREAM) {
                        // TODO: 6/16/2019 need to handle stream
                    }
                }
            };


    private final ConnectionLifecycleCallback connectionLifecycleCallback =
            new ConnectionLifecycleCallback() {
                @Override
                public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                    NODE node = parseNode(endpointId, connectionInfo.getEndpointName());
                    putDeviceNearBy(node);

                    if (debuggable)
                        Log.d(TAG, "onConnectionInitiated " + endpointId + " " + connectionInfo.getEndpointName() + " waiting for approval (accept / reject)");
                    callback.onConnectionInitiated(node, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            putDeviceOnline(endpointId);

                            if (debuggable)
                                Log.d(TAG, "onConnectionSuccess  connected with: " + endpointId);
                            if (callback != null)
                                callback.onConnectionSuccess(findOnlineDeviceById(endpointId), result);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            if (debuggable)
                                Log.d(TAG, "onConnectionRejected  " + endpointId + " rejected connection request");
                            if (callback != null)
                                callback.onConnectionRejected(findAvailableDeviceById(endpointId), result);
                            break;
                        default:
                            Log.e(TAG, "onConnectionFailed when try to connect with " + endpointId + "\t" + result.getStatus().getStatusMessage() + " " + result.getStatus().toString());
                            if (callback != null)
                                callback.onConnectionFailed(findAvailableDeviceById(endpointId), result);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    NODE node = removeDevice(endpointId);
                    if (debuggable) Log.d(TAG, "onDisconnected from " + endpointId);
                    if (callback != null) callback.onDisconnected(node);
                }
            };


    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    NODE node = parseNode(endpointId, info.getEndpointName());
                    putDeviceNearBy(node);

                    if (debuggable)
                        Log.d(TAG, "onDeviceFound  " + info.getEndpointName() + " : " + endpointId);
                    if (callback != null) callback.onDeviceFound(node, info);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    NODE node = removeDevice(endpointId);
                    if (debuggable) Log.d(TAG, "onDeviceLost  " + node);
                    if (callback != null) callback.onDeviceLost(node);
                }
            };

    public void requestConnection(@NonNull String endpointId) {
        try {
            connection.requestConnection(node.toString(), endpointId, connectionLifecycleCallback);
            if (debuggable) Log.d(TAG, "requestConnection to : " + endpointId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestConnection(@NonNull NODE node) {
        requestConnection(node.getEndpointId());
    }

    public void acceptConnection(@NonNull String endpointId) {
        try {
            connection.acceptConnection(endpointId, payloadCallback);
            if (debuggable) Log.d(TAG, "acceptConnection request from : " + endpointId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void acceptConnection(@NonNull NODE node) {
        acceptConnection(node.getEndpointId());
    }

    public void rejectConnection(@NonNull String endpointId) {
        try {
            connection.rejectConnection(endpointId);
            if (debuggable) Log.d(TAG, "rejectConnection request from : " + endpointId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void rejectConnection(@NonNull NODE node) {
        rejectConnection(node.getEndpointId());
    }

    public void startAdvertising(@NonNull String serviceId, @NonNull final Strategy strategy) {
        try {
            connection.startAdvertising(
                    node.toString(), serviceId, connectionLifecycleCallback,
                    new AdvertisingOptions.Builder().setStrategy(strategy).build())
                    .addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            if (debuggable)
                                Log.d(TAG, "onAdvertiseCanceled name : " + node.toString() + " strategy: " + strategy.toString());
                            if (callback != null) callback.onAdvertiseCanceled();
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "onAdvertiseSuccess name : " + node.toString() + " strategy: " + strategy.toString());
                            if (callback != null) callback.onAdvertiseSuccess();
                        }
                    })
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (debuggable)
                                Log.d(TAG, "onAdvertiseComplete name : " + node.toString() + " strategy: " + strategy.toString());
                            if (callback != null) callback.onAdvertiseComplete();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onAdvertiseFailed name : " + node.toString() + " strategy: " + strategy.toString() + " error: " + e.getMessage());
                            if (callback != null) callback.onAdvertiseFailed(e);
                        }
                    });

            if (debuggable)
                Log.d(TAG, "onStartAdvertising  name: " + node.toString() + " serviceId: " + serviceId + " strategy: " + strategy.toString());
//            if (callback != null) callback.onStartAdvertising(name, serviceId, strategy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAdvertising() {
        try {
            connection.stopAdvertising();
            if (debuggable) Log.d(TAG, "onStopAdvertising");
            if (callback != null) callback.onStopAdvertising();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAllEndpoints() {
        try {
            connection.stopAllEndpoints();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnectFrom(String endPointId) {
        try {
            connection.disconnectFromEndpoint(endPointId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void disconnectFrom(@NonNull NODE node) {
        disconnectFrom(node.getEndpointId());
    }

    public void startDiscovery(final @NonNull String serviceId, final @NonNull Strategy strategy) {
        try {

            connection.startDiscovery(
                    serviceId, endpointDiscoveryCallback,
                    new DiscoveryOptions.Builder().setStrategy(strategy).build())
                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (debuggable)
                                Log.d(TAG, "onDiscoveryComplete serviceId : " + serviceId + " strategy: " + strategy.toString());
                            if (callback != null) callback.onDiscoveryComplete();
                        }
                    })
                    .addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            if (debuggable)
                                Log.d(TAG, "onDiscoveryCanceled serviceId : " + serviceId + " strategy: " + strategy.toString());
                            if (callback != null) callback.onDiscoveryCanceled();
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "onDiscoverySuccess serviceId : " + serviceId + " strategy: " + strategy.toString());
                            if (callback != null) callback.onDiscoverySuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "onAdvertiseFailed  strategy: " + strategy.toString() + " error: " + e.getMessage());
                            if (callback != null) callback.onDiscoveryFailed(e);
                        }
                    });

            if (debuggable)
                Log.d(TAG, "onStartDiscovery   serviceId: " + serviceId + " strategy: " + strategy.toString());
//            if (callback != null) callback.onStartDiscovery(serviceId, strategy);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopDiscovery() {
        try {
            connection.stopDiscovery();
            if (debuggable) Log.d(TAG, "onStopDiscovery");
            if (callback != null) callback.onStopDiscovery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(@NonNull NODE node, @NonNull File file) throws Exception {
        sendFile(node.getEndpointId(), file);
    }


    public void sendFile(@NonNull final String endpointId, @NonNull File file) throws Exception {
        FilePayload payload = new FilePayload(file);
        Payload namePayload = payload.toMsgFileMeta();
        final Payload filePayload = payload.toFilePayload();

        connection.sendPayload(endpointId, namePayload).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                connection.sendPayload(endpointId, filePayload);
            }
        });

    }

    public Task<Void> sendMessage(@NonNull NODE node, @NonNull String message) {
        return sendMessage(node.getEndpointId(), message);
    }

    public Task<Void> sendMessage(@NonNull String endpointId, @NonNull String message) {
        return connection.sendPayload(endpointId, Payload.fromBytes(message.getBytes()));
    }

}
