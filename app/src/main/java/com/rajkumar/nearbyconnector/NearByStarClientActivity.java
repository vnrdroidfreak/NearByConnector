package com.rajkumar.nearbyconnector;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Strategy;
import com.virudhairaj.nearby.NBCallback;
import com.virudhairaj.nearby.NBConnector;
import com.virudhairaj.nearby.Node;
import com.virudhairaj.nearby.NodeParser;


public class NearByStarClientActivity extends AppCompatActivity {
    private static final String TAG = "NearByStarClientActivity";

    NBConnector<Node> starSlave = null;
    Node me;

    NBCallback<Node> callback = new NBCallback<Node>() {
        @Override
        public void onConnectionInitiated(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull ConnectionInfo connectionInfo) {
            connector.acceptConnection(node);
        }

        @Override
        public void onNodeFound(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull DiscoveredEndpointInfo info) {
            connector.stopDiscovery();
            connector.requestConnection(node);
        }

        @Override
        public void onConnectionSuccess(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull ConnectionResolution result) {
            connector.sendMessage(node, "test message");
        }
    };
    NodeParser<Node> nodeParser = new NodeParser<Node>() {
        @NonNull
        @Override
        public Node parseNode(@NonNull String endpointId, @NonNull String json) {
            return new Node(json, endpointId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        me = new Node(this, "Client", Node.Type.slave);
        starSlave = new NBConnector<>(this, me, nodeParser, callback);
        starSlave.startDiscovery(getPackageName(), Strategy.P2P_STAR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        starSlave.stopAllEndpoints();
    }
}
