package com.rajkumar.nearbyconnector;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;
import com.virudhairaj.nearby.NBConnector;
import com.virudhairaj.nearby.Node;


public class NearByServerActivity extends AppCompatActivity {
    private static final String TAG = "NearByServerActivity";

    private NBConnector<Node> starMaster;
    private Node me;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        me = new Node(this, "Client", Node.Type.master);

        starMaster = new NBConnector<Node>(this, me) {
            @NonNull
            @Override
            public Node parseNode(@NonNull String endpointId, @NonNull String json) {
                return new Node(json).setEndpointId(endpointId);
            }

            @Override
            public void onConnectionInitiated(@NonNull Node node, @NonNull ConnectionInfo connectionInfo) {
                starMaster.acceptConnection(node);
            }

            @Override
            public void onConnectionSuccess(@NonNull Node node, @NonNull ConnectionResolution result) {
                starMaster.sendMessage(node, "test message");
            }
        };

        starMaster.startAdvertising(getPackageName(), Strategy.P2P_STAR);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        starMaster.stopAllEndpoints();
        starMaster.stopAdvertising();
    }
}
