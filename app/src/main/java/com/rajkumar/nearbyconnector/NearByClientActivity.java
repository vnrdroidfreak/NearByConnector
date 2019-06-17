package com.rajkumar.nearbyconnector;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Strategy;
import com.virudhairaj.nearby.NBConnector;
import com.virudhairaj.nearby.Node;


public class NearByClientActivity extends AppCompatActivity {
    private static final String TAG = "NearByClientActivity";

    NBConnector<Node> starSlave = null;
    Node me;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        me = new Node(this, "Client", Node.Type.slave);

        starSlave = new NBConnector<Node>(this, me) {
            @NonNull
            @Override
            public Node parseNode(@NonNull String endpointId, @NonNull String json) {
                return new Node(json).setEndpointId(endpointId);
            }

            @Override
            public void onDiscoverySuccess() {
                starSlave.stopDiscovery();
            }

            @Override
            public void onConnectionInitiated(@NonNull Node node, @NonNull ConnectionInfo connectionInfo) {

                starSlave.acceptConnection(node);
            }

            @Override
            public void onDeviceFound(@NonNull Node node, @NonNull DiscoveredEndpointInfo info) {
                starSlave.requestConnection(node);
            }

            @Override
            public void onConnectionSuccess(@NonNull Node node, @NonNull ConnectionResolution result) {
                starSlave.sendMessage(node, "test message");
            }
        };

        starSlave.startDiscovery(getPackageName(), Strategy.P2P_STAR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        starSlave.stopAllEndpoints();
    }
}
