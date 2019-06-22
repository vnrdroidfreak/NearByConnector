package com.rajkumar.nearbyconnector;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.Strategy;
import com.virudhairaj.nearby.NBCallback;
import com.virudhairaj.nearby.NBConnector;
import com.virudhairaj.nearby.NBNode;
import com.virudhairaj.nearby.NBNodeParser;

import java.util.ArrayList;


public class NearByStarServerActivity extends AppCompatActivity {
    private static final String TAG = "NearByStarServerActivity";

    private NBConnector<Node> starMaster;
    private Node me;

    private ArrayList<Node> connected = new ArrayList<>();
    private ArrayList<Node> waitingList = new ArrayList<>();

    NodesAdapter connectedAdapter,waitingAdapter;
    RecyclerView connectedRecycler,waitingRecycler;

    NBCallback<Node> callback = new NBCallback<Node>() {
        @Override
        public void onConnectionInitiated(@NonNull final NBConnector<Node> connector, @NonNull final Node node, @NonNull ConnectionInfo connectionInfo) {
            waitingList.add(node);
            waitingAdapter.notifyDataSetChanged();
            AlertDialog.Builder builder = new AlertDialog.Builder(NearByStarServerActivity.this);
            builder.setTitle("Connection Request");
            builder.setMessage("from  "+node);
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
        public void onConnectionSuccess(@NonNull NBConnector<Node> connector, @NonNull Node node, @NonNull ConnectionResolution result) {
            waitingList.remove(node);
            waitingAdapter.notifyDataSetChanged();
            node.setConnected(true);
            connected.add(node);
            connectedAdapter.notifyDataSetChanged();

            connector.sendMessage(node, "test message");
        }

        @Override
        public void onDisconnected(@NonNull NBConnector<Node> connector, @NonNull Node node) {
            connected.remove(node);
            connectedAdapter.notifyDataSetChanged();
        }
    };

    NBNodeParser<Node> nodeParser = new NBNodeParser<Node>() {
        @NonNull
        @Override
        public Node parseNode(@NonNull String endpointId, @NonNull String json) {
            return new Node(json, endpointId);
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_star_server);

        me = new Node(this, "Server", NBNode.Type.master);
        starMaster = new NBConnector<>(this, me, nodeParser, callback);
        starMaster.startAdvertising(getPackageName(), Strategy.P2P_STAR);


        waitingRecycler=findViewById(R.id.connectedRecycler);
        waitingAdapter=new NodesAdapter(waitingList, new ItemClickListener<Node>() {
            @Override
            public void onItemClick(View view, @NonNull Node node, int position, Object... extras) {
                switch (view.getId()){
                    case R.id.btnConnect:
                        starMaster.requestConnection(node);
                        break;
                }
            }
        });

        waitingRecycler.setLayoutManager(new LinearLayoutManager(this));
        waitingRecycler.setAdapter(connectedAdapter);



        connectedRecycler=findViewById(R.id.connectedRecycler);
        connectedAdapter=new NodesAdapter(connected, new ItemClickListener<Node>() {
            @Override
            public void onItemClick(View view, @NonNull Node node, int position, Object... extras) {
                switch (view.getId()){
                    case R.id.btnDisConnect:
                        starMaster.disconnectFrom(node);
                        break;
                    case R.id.btnSendMsg:
                        starMaster.sendMessage(node,"hi");
                        break;
                }
            }
        });

        connectedRecycler.setLayoutManager(new LinearLayoutManager(this));
        connectedRecycler.setAdapter(connectedAdapter);


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        starMaster.stopAllEndpoints();
        starMaster.stopAdvertising();
    }
}
