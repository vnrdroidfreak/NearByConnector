package com.virudhairaj.nearby;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.SimpleArrayMap;
import android.text.TextUtils;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public abstract class NBConnector<NODE extends Node> {
    private static final String TAG = NBConnector.class.getSimpleName();
    private final ConnectionsClient connection;
    private final NODE node;
    public static boolean debuggable = true;

    private final SimpleArrayMap<Long, Payload> incomingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, Payload> outgoingPayloads = new SimpleArrayMap<>();
    private final SimpleArrayMap<Long, String> fileNamePayloads = new SimpleArrayMap<>();

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
            if (debuggable) Log.e(TAG, e.getMessage());
        }
    }

    private void putDeviceOnline(@NonNull NODE node) {
        try {
            nearBy.remove(node.getEndpointId());
            connected.put(node.getEndpointId(), node);
        } catch (Exception e) {
            if (debuggable) Log.e(TAG, e.getMessage());
        }
    }

    private void putDeviceOnline(@NonNull String endpointId) {
        try {
            final NODE node = nearBy.remove(endpointId);
            if (node != null)
                connected.put(endpointId, node);
        } catch (Exception e) {
            if (debuggable) Log.e(TAG, e.getMessage());
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

    private static final String[] PERMISSIONS =
            new String[]{
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
            };

    @NonNull
    public abstract NODE parseNode(@NonNull String endpointId, @NonNull String json);


    //------------callback methods

    public void onStopDiscovery() {
    }

    public void onStopAdvertising() {
    }

    public void onAdvertiseCanceled() {
    }

    public void onAdvertiseSuccess() {
    }

    public void onAdvertiseFailed(final @NonNull Exception e) {
    }

    public void onDiscoveryCanceled() {
    }

    public void onDiscoverySuccess() {
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

    public void onMessageReceived(final @NonNull NODE from, final @NonNull Message message) {
    }

    public void onIncomingFile(final @NonNull NODE node, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransfer(final @NonNull NODE node, boolean isIncoming, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransferCanceled(final @NonNull NODE node, boolean isIncoming, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileTransferFailed(final @NonNull NODE node, boolean isIncoming, final @NonNull Payload payload, final String fileName) {
    }

    public void onFileReceived(final @NonNull NODE node, final @NonNull File file) {
    }


    //end of callback methods--------------------------------------------------


    private String getRejectedRequiredPermissions(@NonNull Context context) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < PERMISSIONS.length; i++) {
            final String permission = PERMISSIONS[i];
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                sb.append(permission);
                if (i != 0 && PERMISSIONS.length > 1 && i < PERMISSIONS.length - 1) {
                    sb.append("\n");
                }
            }
        }
        return sb.toString();
    }

    public NBConnector(@NonNull final ConnectionsClient connection, @NonNull final NODE myNode) {
        this.connection = connection;
        this.node = myNode;
        connected = new HashMap<>();
        nearBy = new HashMap<>();
    }

    public NBConnector(@NonNull final Context context, @NonNull final NODE myNode) {
        if (context == null) throw new RuntimeException("Context is null");
        if (myNode == null) throw new RuntimeException("myNode is null");
        String rejectedPermissions = getRejectedRequiredPermissions(context);
        if (!TextUtils.isEmpty(rejectedPermissions))
            throw new RuntimeException("Required permissions not granted " + rejectedPermissions);
        this.connection = Nearby.getConnectionsClient(context);
        this.node = myNode;
        connected = new HashMap<>();
        nearBy = new HashMap<>();
    }


    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    final NODE node = connected.get(endpointId);
                    if (debuggable) Log.d(TAG, "onPayloadReceived from " + node);
                    incomingPayloads.put(payload.getId(), payload);

                    final String content = new String(payload.asBytes(), StandardCharsets.UTF_8);
                    if (payload.getType() == Payload.Type.BYTES && content.contains(MsgType.fileMeta.name())) {
                        MsgFileMeta meta = MsgFileMeta.fromJsonStr(content);
                        if (debuggable)
                            Log.d(TAG, "onIncomingFile: from " + node + " fileName: " + meta.content);
                        if (node != null)
                            onIncomingFile(node, payload, meta.content);
                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    boolean isIncoming = incomingPayloads.containsKey(update.getPayloadId());
                    boolean isOutgoing = outgoingPayloads.containsKey(update.getPayloadId());

                    if (!isIncoming && !isOutgoing) {
                        if (debuggable)
                            Log.i(TAG, "onPayloadTransferUpdate: payload " + update.getPayloadId() + " not found in local map");
                        return;
                    }

                    if (!connected.containsKey(endpointId)) {
                        if (debuggable)
                            Log.i(TAG, "onPayloadTransferUpdate: endpoint " + endpointId + " not found in connected list");
                        return;
                    }


                    final Payload payload = isIncoming ? incomingPayloads.get(update.getPayloadId()) : outgoingPayloads.get(update.getPayloadId());
                    final NODE node = connected.get(endpointId);

                    switch (payload.getType()) {
                        case Payload.Type.BYTES: {
                            if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                                final String content = new String(payload.asBytes(), StandardCharsets.UTF_8);

                                if (isIncoming) incomingPayloads.remove(payload);
                                else outgoingPayloads.remove(payload);

                                if (isIncoming && content.contains(MsgType.fileMeta.name())) {
                                    if (debuggable)
                                        Log.d(TAG, "onFileMetaReceived: from : " + node + " content: " + content);
                                    MsgFileMeta meta = MsgFileMeta.fromJsonStr(content);
                                    fileNamePayloads.put(payload.getId(), meta.content);
                                } else if (isIncoming) {
                                    if (debuggable)
                                        Log.d(TAG, "onMessageReceived: from : " + node + " content: " + content);

                                    onMessageReceived(node, new Message(content) {
                                    });
                                } else {
                                    //out going messages
                                }
                            }
                        }
                        break;

                        case Payload.Type.FILE: {
                            if (update.getStatus() == PayloadTransferUpdate.Status.IN_PROGRESS) {

                                onFileTransfer(node, isIncoming, payload, fileNamePayloads.get(payload.getId()));
                            } else if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                                final Payload completed = isIncoming ? incomingPayloads.remove(payload.getId()) : outgoingPayloads.remove(payload.getId());
                                fileNamePayloads.remove(payload.getId());

                                onFileReceived(node, payload.asFile().asJavaFile());
                            } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                                final Payload canceled = isIncoming ? incomingPayloads.remove(payload.getId()) : outgoingPayloads.remove(payload.getId());
                                final String fileName = fileNamePayloads.remove(payload.getId());
                                if (canceled != null)
                                    onFileTransferCanceled(node, isIncoming, canceled, fileName);
                            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                                final Payload failed = isIncoming ? incomingPayloads.remove(payload.getId()) : outgoingPayloads.remove(payload.getId());
                                final String fileName = fileNamePayloads.remove(payload.getId());
                                if (failed != null)
                                    onFileTransferFailed(node, isIncoming, failed, fileName);
                            }
                        }
                        break;
                        case Payload.Type.STREAM: {
                            // TODO: 6/16/2019 need to handle stream
                        }
                        break;
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
                    NBConnector.this.onConnectionInitiated(node, connectionInfo);
                }

                @Override
                public void onConnectionResult(String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            putDeviceOnline(endpointId);

                            if (debuggable)
                                Log.d(TAG, "onConnectionSuccess  connected with: " + endpointId);

                            NBConnector.this.onConnectionSuccess(findOnlineDeviceById(endpointId), result);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            if (debuggable)
                                Log.d(TAG, "onConnectionRejected  " + endpointId + " rejected connection request");

                            NBConnector.this.onConnectionRejected(findAvailableDeviceById(endpointId), result);
                            break;
                        default:
                            if (debuggable)
                                Log.e(TAG, "onConnectionFailed when try to connect with " + endpointId + "\t" + result.getStatus().getStatusMessage() + " " + result.getStatus().toString());

                            NBConnector.this.onConnectionFailed(findAvailableDeviceById(endpointId), result);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    NODE node = removeDevice(endpointId);
                    if (debuggable) Log.d(TAG, "onDisconnected from " + endpointId);
                    NBConnector.this.onDisconnected(node);
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
                    NBConnector.this.onDeviceFound(node, info);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    NODE node = removeDevice(endpointId);
                    if (debuggable) Log.d(TAG, "onDeviceLost  " + node);
                    NBConnector.this.onDeviceLost(node);
                }
            };

    public void requestConnection(@NonNull final String endpointId) {
        try {
            connection.requestConnection(node.toString(), endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "requestConnection => status: success\trequest to " + endpointId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "requestConnection => status: failed\trequest to " + endpointId + "\t" + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void requestConnection(@NonNull NODE node) {
        requestConnection(node.getEndpointId());
    }

    public void acceptConnection(@NonNull final String endpointId) {
        try {
            connection.acceptConnection(endpointId, payloadCallback)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "acceptConnection => status: success\trequest from " + endpointId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "acceptConnection => status: failed\trequest from " + endpointId + " " + e.getMessage());
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void acceptConnection(@NonNull NODE node) {
        acceptConnection(node.getEndpointId());
    }

    public void rejectConnection(@NonNull final String endpointId) {
        try {
            connection.rejectConnection(endpointId)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "rejectConnection => status: success\trequest from " + endpointId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (debuggable)
                                Log.d(TAG, "rejectConnection => status: failed\trequest from " + endpointId + " " + e.getMessage());
                        }
                    });
//            if (debuggable) Log.d(TAG, "rejectConnection request from : " + endpointId);
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

                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "startAdvertising => status: success\tname : " + node.toString() + " strategy: " + strategy.toString());
                            NBConnector.this.onAdvertiseSuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "startAdvertising => status: failed\tname : " + node.toString() + " strategy: " + strategy.toString() + " error: " + e.getMessage());
                            NBConnector.this.onAdvertiseFailed(e);
                        }
                    })
                    .addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            if (debuggable)
                                Log.d(TAG, "startAdvertising => status: canceled\tname : " + node.toString() + " strategy: " + strategy.toString());
                            NBConnector.this.onAdvertiseCanceled();
                        }
                    });
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            if (debuggable)
//                                Log.d(TAG, "startAdvertising => status: complete\tname : " + node.toString() + " strategy: " + strategy.toString());
//                        }
//                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAdvertising() {
        try {
            connection.stopAdvertising();
            if (debuggable) Log.d(TAG, "stopAdvertising");
            NBConnector.this.onStopAdvertising();
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
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "startDiscovery => success\tserviceId : " + serviceId + " strategy: " + strategy.toString());
                            NBConnector.this.onDiscoverySuccess();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (debuggable)
                                Log.e(TAG, "startDiscovery => failed\tserviceId : " + serviceId + " strategy: " + strategy.toString() + " error: " + e.getMessage());
                            NBConnector.this.onDiscoveryFailed(e);
                        }
                    })
                    .addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            if (debuggable)
                                Log.d(TAG, "startDiscovery => canceled\tserviceId : " + serviceId + " strategy: " + strategy.toString());
                            NBConnector.this.onDiscoveryCanceled();
                        }
                    });
//                    .addOnCompleteListener(new OnCompleteListener<Void>() {
//                        @Override
//                        public void onComplete(@NonNull Task<Void> task) {
//                            if (debuggable)
//                                Log.d(TAG, "startDiscovery => complete\tserviceId : " + serviceId + " strategy: " + strategy.toString());
//                        }
//                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopDiscovery() {
        try {
            connection.stopDiscovery();
            if (debuggable) Log.d(TAG, "onStopDiscovery");
            NBConnector.this.onStopDiscovery();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(@NonNull final NODE node, @NonNull final File file) throws Exception {
        final FilePayload payload = new FilePayload(file);
        Payload namePayload = payload.toMsgFileMeta();
        final Payload filePayload = payload.toFilePayload();

        connection.sendPayload(node.getEndpointId(), namePayload).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                outgoingPayloads.put(filePayload.getId(), filePayload);
                connection.sendPayload(node.getEndpointId(), filePayload)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                if (debuggable)
                                    Log.d(TAG, "sendFile => request success\t  " + node + " file: " + file.getAbsolutePath());
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.e(TAG, "sendFile => request failed\t  " + node + " file: " + file.getAbsolutePath() + " error: " + e.getMessage());
                            }
                        });
            }
        })
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (debuggable)
                            Log.d(TAG, "sendFileMeta => success\t  " + node + " file: " + file.getAbsolutePath());
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "sendFileMeta => failed\t  " + node + " file: " + file.getAbsolutePath() + " error: " + e.getMessage());
                    }
                });
    }


    public void sendFile(@NonNull final String endpointId, @NonNull File file) throws Exception {
        if (!connected.containsKey(endpointId)) {
            Log.e(TAG, "sendFile => failed\tendpointId " + endpointId + " not found in connected list");
            return;
        }
        sendFile(connected.get(endpointId), file);
    }

    public void sendMessage(@NonNull final NODE node, @NonNull final String message) {
        connection.sendPayload(node.getEndpointId(), Payload.fromBytes(message.getBytes()))
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        if (debuggable)
                            Log.d(TAG, "sendMessage => success\t  " + node + " message: " + message);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "sendMessage => failed\t  " + node + " message: " + message + " error: " + e.getMessage());
                    }
                });
    }

    public void sendMessage(@NonNull String endpointId, @NonNull String message) {
        if (!connected.containsKey(endpointId)) {
            Log.e(TAG, "sendMessage => failed\tendpointId " + endpointId + " not found in connected list");
            return;
        }
        sendMessage(connected.get(endpointId), message);
    }

    public void sendMessage(@NonNull final NODE node, @NonNull final Message message) {
        sendMessage(node.getEndpointId(), message.toString());
    }


    // TODO: 6/17/2019 need to handle this 
    public void abortFileTransfer(@NonNull final String endpointId, final long payloadId) {
        final boolean isIncoming = incomingPayloads.containsKey(payloadId);
        final boolean isOutgoing = outgoingPayloads.containsKey(payloadId);

        if (!isIncoming && !isOutgoing) {
            Log.e(TAG, "abortFileTransfer => failed\tpayload " + payloadId + " not found in local map");
            return;
        }

        if (!connected.containsKey(endpointId)) {
            Log.e(TAG, "abortFileTransfer => failed\tendpointId " + endpointId + " not found in connected list");
            return;
        }

        final String fileName = fileNamePayloads.get(payloadId);
        final NODE node = connected.get(endpointId);

        connection.cancelPayload(payloadId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        final Payload remove = isIncoming ? incomingPayloads.remove(payloadId) : outgoingPayloads.remove(payloadId);
                        fileNamePayloads.remove(payloadId);
                        if (debuggable)
                            Log.d(TAG, "abortFileTransfer => success\t  " + node + " fileName: " + fileName);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "abortFileTransfer => failed\t  " + node + " fileName: " + fileName + " error: " + e.getMessage());
                    }
                });
    }
}
