package com.rajkumar.nearbyconnector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.Strategy;
import com.virudhairaj.nearby.NBCallback;
import com.virudhairaj.nearby.NBConnector;
import com.virudhairaj.nearby.NBNodeParser;

import java.util.ArrayList;


public class NearByStarClientActivity extends AppCompatActivity {
    private static final String TAG = "NearByStarClientActivity";

    NBConnector<Node> starSlave = null;
    Node me, server;

    NodesAdapter.ItemHolder serverHolder;
    private ArrayList<Node> data = new ArrayList<>();
    RecyclerView recycler;
    NodesAdapter adapter;
    NBCallback<Node> callback = new NBCallback<Node>() {
        @Override
        public void onConnectionInitiated(@NonNull final NBConnector<Node> connector, @NonNull final Node node, @NonNull ConnectionInfo connectionInfo) {
//            int index = data.indexOf(node);
//            if (index >= 0) {
//                data.get(index).
//            }

            AlertDialog.Builder builder = new AlertDialog.Builder(NearByStarClientActivity.this);
            builder.setTitle("Connection Request");
            builder.setMessage("from  " + node);
            String positiveText = getString(android.R.string.ok);
            builder.setPositiveButton(positiveText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            connector.acceptConnection(node);
                            dialog.dismiss();
                        }
                    });

            String negativeText = getString(android.R.string.cancel);
            builder.setNegativeButton(negativeText,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            connector.rejectConnection(node);

                        }
                    });

            AlertDialog dialog = builder.create();
// display dialog
            dialog.show();
        }

        @Override
        public void onNodeFound(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull DiscoveredEndpointInfo info) {
            connector.stopDiscovery();
            server = node;
            me.setConnected(false);
            connector.requestConnection(node);
        }


        @Override
        public void onNodeLost(@NonNull NBConnector<Node> connector, @NonNull Node node) {
            me.setConnected(false);
            server = null;
        }

        @Override
        public void onConnectionSuccess(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull ConnectionResolution result) {
            me.setConnected(true);
            connector.sendMessage(node, "test message");
        }

        @Override
        public void onDisconnected(@NonNull NBConnector<Node> connector, @NonNull Node node) {
            me.setConnected(false);
            server = null;
        }
    };

    NBNodeParser<Node> NBNodeParser = new NBNodeParser<Node>() {
        @NonNull
        @Override
        public Node parseNode(@NonNull String endpointId, @NonNull String json) {
            return new Node(json, endpointId);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_client);

        serverHolder = new NodesAdapter.ItemHolder(findViewById(R.id.myNode));
        serverHolder.root.setVisibility(View.GONE);

        me = new Node(this, "Client", Node.Type.slave);

        final View myNode = findViewById(R.id.myNode);
        ((TextView) myNode.findViewById(R.id.txtName)).setText(me.getName());
        ((TextView) myNode.findViewById(R.id.txtEndpointId)).setText(me.getEndpointId());
        ((TextView) myNode.findViewById(R.id.txtType)).setText(me.getType().name());


        starSlave = new NBConnector<>(this, me, NBNodeParser, callback);
        starSlave.startDiscovery(getPackageName(), Strategy.P2P_STAR);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        starSlave.stopAllEndpoints();
    }
}
