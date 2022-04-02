package com.rajkumar.nearbyconnector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;


public class NodesAdapter extends RecyclerView.Adapter<NodesAdapter.ItemHolder> {

    ArrayList<Node> data;
    ItemClickListener<Node> clickListener=null;

    public NodesAdapter(ArrayList<Node> data ,ItemClickListener<Node> clickListener) {
        this.data = data != null ? data : new ArrayList<Node>();
        this.clickListener=clickListener;
    }

    public void setData(ArrayList<Node> data, boolean clearAll) {
        if (this.data == null) this.data = new ArrayList<>();
        if (clearAll) this.data.clear();
        int start = data.size();
        this.data.addAll(data);
        if (clearAll) notifyDataSetChanged();
        else notifyItemRangeInserted(start, data.size());
    }

    @NonNull
    @Override
    public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return ItemHolder.newHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull final ItemHolder itemHolder, final int position) {
        final Node item = data.get(position);
        if(itemHolder instanceof ItemHolder){
            ((ItemHolder) itemHolder).bind(item,position,clickListener);
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    public static class ItemHolder extends RecyclerView.ViewHolder {
        public final View root;
        public final TextView txtName,txtEndpointId;
        public final Button btnConnect,btnDisConnect,btnSendMsg;
        public ItemHolder(View itemView) {
            super(itemView);
            root=itemView;
            txtName=itemView.findViewById(R.id.txtName);
            txtEndpointId=itemView.findViewById(R.id.txtEndpointId);
            btnConnect=itemView.findViewById(R.id.btnConnect);
            btnDisConnect=itemView.findViewById(R.id.btnDisConnect);
            btnSendMsg=itemView.findViewById(R.id.btnSendMsg);
        }

        public void bind(final Node item,final int position,final ItemClickListener<Node> clickListener){

            txtName.setText(item.getName());
            txtEndpointId.setText(item.getEndpointId());
            btnConnect.setVisibility(item.isConnected()?View.GONE:View.VISIBLE);
            btnDisConnect.setVisibility(!item.isConnected()?View.GONE:View.VISIBLE);
            btnSendMsg.setVisibility(!item.isConnected()?View.GONE:View.VISIBLE);


            btnConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickListener!=null)clickListener.onItemClick(v,item,position);
                }
            });

            btnDisConnect.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickListener!=null)clickListener.onItemClick(v,item,position);
                }
            });

            btnSendMsg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(clickListener!=null)clickListener.onItemClick(v,item,position);
                }
            });
        }

        public static ItemHolder newHolder(@NonNull ViewGroup parent) {
            return new ItemHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.node_item, parent, false)
            );
        }
    }
}
