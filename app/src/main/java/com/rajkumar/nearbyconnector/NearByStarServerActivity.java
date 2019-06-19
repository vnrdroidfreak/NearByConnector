package com.rajkumar.nearbyconnector;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;
import com.virudhairaj.nearby.NBCallback;
import com.virudhairaj.nearby.NBConnector;
import com.virudhairaj.nearby.Node;
import com.virudhairaj.nearby.NodeParser;


public class NearByStarServerActivity extends AppCompatActivity {
    private static final String TAG = "NearByStarServerActivity";

    private NBConnector<Node> starMaster;
    private Node me;


    NBCallback<Node> callback = new NBCallback<Node>() {
        @Override
        public void onConnectionInitiated(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull ConnectionInfo connectionInfo) {
            connector.acceptConnection(node);
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
            return new Node(json,endpointId);
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        me = new Node(this, "Client", Node.Type.master);

        starMaster = new NBConnector<>(this,me,nodeParser,callback);

        starMaster.startAdvertising(getPackageName(), Strategy.P2P_STAR);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        starMaster.stopAllEndpoints();
        starMaster.stopAdvertising();
    }
}
