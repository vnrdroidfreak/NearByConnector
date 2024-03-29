package com.virudhairaj.nearby;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.core.content.ContextCompat;
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

public class NBConnector<NODE extends NBNode> {
    public static final String TAG = NBConnector.class.getSimpleName();
    public static boolean debuggable = true;
    private final ConnectionsClient connection;
    private final NODE myNode;
    private NBCallback<NODE> callback;
    private NBNodeParser<NODE> nodeParser = null;
    private final NBConnector THIS;
    private final Context context;

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
            };

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

    private boolean hasReadStoragePermission(@NonNull Context context){
        return ContextCompat.checkSelfPermission(context,  Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasWriteStoragePermission(@NonNull Context context){
        return ContextCompat.checkSelfPermission(context,  Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED;
    }

    public NBConnector(@NonNull final Context context, @NonNull final NODE myNode, @NonNull final NBNodeParser<NODE> nodeParser, @Nullable final NBCallback<NODE> callback) {
        if (context == null) throw new RuntimeException("Context is null");
        if (myNode == null) throw new RuntimeException("myNode is null");
        this.context=context;
        String rejectedPermissions = getRejectedRequiredPermissions(context);
        if (!TextUtils.isEmpty(rejectedPermissions))
            throw new RuntimeException("Required permissions not granted " + rejectedPermissions);
        this.connection = Nearby.getConnectionsClient(context);
        this.myNode = myNode;
        this.nodeParser = nodeParser;
        connected = new HashMap<>();
        nearBy = new HashMap<>();
        THIS = this;
        this.callback = callback;
    }


    private final PayloadCallback payloadCallback =
            new PayloadCallback() {
                @Override
                public void onPayloadReceived(String endpointId, Payload payload) {
                    final NODE node = connected.get(endpointId);
//                    if (debuggable) Log.d(TAG, "onPayloadReceived from " + node);
                    incomingPayloads.put(payload.getId(), payload);

                    if (payload.getType() == Payload.Type.BYTES) {
                        final String content = new String(payload.asBytes(), StandardCharsets.UTF_8);
                        if (content.contains(MsgType.fileMeta.name())) {
                            MsgFileMeta meta = MsgFileMeta.fromJsonStr(content);
                            if (debuggable)
                                Log.d(TAG, "onIncomingFile: from " + node + " fileName: " + meta.content);
                            if (node != null)
                                if (callback != null)
                                    callback.onFileIncoming(THIS, node, payload, meta.content);
                        }

                    }
                }

                @Override
                public void onPayloadTransferUpdate(String endpointId, PayloadTransferUpdate update) {
                    boolean isIncoming = incomingPayloads.containsKey(update.getPayloadId());
                    boolean isOutgoing = outgoingPayloads.containsKey(update.getPayloadId());

                    if (!isIncoming && !isOutgoing) {
                        if (debuggable)
                            Log.i(TAG, "onPayloadTransferUpdate: payloadId " + update.getPayloadId() + " not found in local map");
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

                                    if (callback != null)
                                        callback.onMessageReceived(THIS, node, new Message(content) {
                                        });
                                } else {
                                    //out going messages
                                }
                            }
                        }
                        break;

                        case Payload.Type.FILE: {
                            if (update.getStatus() == PayloadTransferUpdate.Status.IN_PROGRESS) {

                                if (callback != null)
                                    callback.onFileTransfer(THIS, node, isIncoming, payload, fileNamePayloads.get(payload.getId()));
                            } else if (update.getStatus() == PayloadTransferUpdate.Status.SUCCESS) {
                                final Payload completed = isIncoming ? incomingPayloads.remove(payload.getId()) : outgoingPayloads.remove(payload.getId());
                                fileNamePayloads.remove(payload.getId());

                                if (callback != null)
                                    callback.onFileReceived(THIS, node, payload.asFile().asJavaFile());
                            } else if (update.getStatus() == PayloadTransferUpdate.Status.CANCELED) {
                                final Payload canceled = isIncoming ? incomingPayloads.remove(payload.getId()) : outgoingPayloads.remove(payload.getId());
                                final String fileName = fileNamePayloads.remove(payload.getId());
                                if (canceled != null)
                                    if (callback != null)
                                        callback.onFileTransferCanceled(THIS, node, isIncoming, canceled, fileName);
                            } else if (update.getStatus() == PayloadTransferUpdate.Status.FAILURE) {
                                final Payload failed = isIncoming ? incomingPayloads.remove(payload.getId()) : outgoingPayloads.remove(payload.getId());
                                final String fileName = fileNamePayloads.remove(payload.getId());
                                if (failed != null)
                                    if (callback != null)
                                        callback.onFileTransferFailed(THIS, node, isIncoming, failed, fileName);
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
                public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
                    NODE node = nodeParser.parseNode(endpointId, connectionInfo.getEndpointName());
                    putDeviceNearBy(node);

                    if (debuggable)
                        Log.d(TAG, "onConnectionInitiated " + endpointId + " " + connectionInfo.getEndpointName() + " waiting for approval (accept / reject)");
                    if (callback != null)
                        callback.onConnectionInitiated(THIS, node, connectionInfo);
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            putDeviceOnline(endpointId);

                            if (debuggable)
                                Log.d(TAG, "onConnectionSuccess  connected with: " + endpointId);

                            if (callback != null)
                                callback.onConnectionSuccess(THIS, findOnlineDeviceById(endpointId), result);
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            if (debuggable)
                                Log.d(TAG, "onConnectionRejected  " + endpointId + " rejected connection request");

                            if (callback != null)
                                callback.onConnectionRejected(THIS, findAvailableDeviceById(endpointId), result);
                            break;
                        default:
                            if (debuggable)
                                Log.e(TAG, "onConnectionFailed when try to connect with " + endpointId + "\t" + result.getStatus().getStatusMessage() + " " + result.getStatus().toString());

                            if (callback != null)
                                callback.onConnectionFailed(THIS, findAvailableDeviceById(endpointId), result);
                    }
                }

                @Override
                public void onDisconnected(String endpointId) {
                    NODE node = removeDevice(endpointId);
                    if (debuggable) Log.d(TAG, "onDisconnected from " + node);
                    if (callback != null) callback.onDisconnected(THIS, node);
                }
            };


    // Callbacks for finding other devices
    private final EndpointDiscoveryCallback endpointDiscoveryCallback =
            new EndpointDiscoveryCallback() {
                @Override
                public void onEndpointFound(String endpointId, DiscoveredEndpointInfo info) {
                    NODE node = nodeParser.parseNode(endpointId, info.getEndpointName());
                    putDeviceNearBy(node);

                    if (debuggable)
                        Log.d(TAG, "onNodeFound  " + info.getEndpointName() + " : " + endpointId);
                    if (callback != null) callback.onNodeFound(THIS, node, info);
                }

                @Override
                public void onEndpointLost(String endpointId) {
                    NODE node = removeDevice(endpointId);
                    if (debuggable) Log.d(TAG, "onNodeLost  " + node);
                    if (callback != null) callback.onNodeLost(THIS, node);
                }
            };

    public void requestConnection(@NonNull final String endpointId) {
        try {
            connection.requestConnection(myNode.toString(), endpointId, connectionLifecycleCallback)
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
                                Log.d(TAG, "acceptConnection => status: success\tconnection request from " + endpointId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "acceptConnection => status: failed\tconnection request from " + endpointId + " " + e.getMessage());
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
                                Log.d(TAG, "rejectConnection => status: success\tconnection request from " + endpointId);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (debuggable)
                                Log.d(TAG, "rejectConnection => status: failed\tconnection request from " + endpointId + " " + e.getMessage());
                        }
                    });
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
                    myNode.toString(), serviceId, connectionLifecycleCallback,
                    new AdvertisingOptions.Builder().setStrategy(strategy).build())

                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            if (debuggable)
                                Log.d(TAG, "startAdvertising => status: success\tname : " + myNode.toString() + " strategy: " + strategy.toString());
                            if (callback != null) callback.onAdvertiseSuccess(THIS);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "startAdvertising => status: failed\tname : " + myNode.toString() + " strategy: " + strategy.toString() + " error: " + e.getMessage());
                            if (callback != null) callback.onAdvertiseFailed(THIS, e);
                        }
                    })
                    .addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            if (debuggable)
                                Log.d(TAG, "startAdvertising => status: canceled\tname : " + myNode.toString() + " strategy: " + strategy.toString());
                            if (callback != null) callback.onAdvertiseCanceled(THIS);
                        }
                    });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopAdvertising() {
        try {
            connection.stopAdvertising();
            if (debuggable) Log.d(TAG, "stopAdvertising");
            if (callback != null) callback.onStopAdvertising(THIS);
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
                            if (callback != null) callback.onDiscoverySuccess(THIS);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            if (debuggable)
                                Log.e(TAG, "startDiscovery => failed\tserviceId : " + serviceId + " strategy: " + strategy.toString() + " error: " + e.getMessage());
                            if (callback != null) callback.onDiscoveryFailed(THIS, e);
                        }
                    })
                    .addOnCanceledListener(new OnCanceledListener() {
                        @Override
                        public void onCanceled() {
                            if (debuggable)
                                Log.d(TAG, "startDiscovery => canceled\tserviceId : " + serviceId + " strategy: " + strategy.toString());
                            if (callback != null) callback.onDiscoveryCanceled(THIS);
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stopDiscovery() {
        try {
            connection.stopDiscovery();
            if (debuggable) Log.d(TAG, "stopDiscovery");
            if (callback != null) callback.onStopDiscovery(THIS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(@NonNull final NODE node, @NonNull final File file) throws Exception {
        if (!hasReadStoragePermission(context)){
            Log.e(TAG,"Can't send file\t"+Manifest.permission.READ_EXTERNAL_STORAGE+" permission not granted");
            return;
        }
        final FilePayload payload = new FilePayload(file);
        final Payload namePayload = payload.toMsgFileMeta();
        final Payload filePayload = payload.toFilePayload();

        connection.sendPayload(node.getEndpointId(), namePayload).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                outgoingPayloads.put(filePayload.getId(), filePayload);
                connection.sendPayload(node.getEndpointId(), filePayload)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                outgoingPayloads.put(namePayload.getId(), namePayload);
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
        final Payload payload = Payload.fromBytes(message.getBytes());
        connection.sendPayload(node.getEndpointId(), payload)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        outgoingPayloads.put(payload.getId(), payload);
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
