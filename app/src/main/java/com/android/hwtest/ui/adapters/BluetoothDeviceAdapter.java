package com.android.hwtest.ui.adapters;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.android.hwtest.R;
import com.android.hwtest.models.BluetoothDevice;

import java.util.ArrayList;

public class BluetoothDeviceAdapter extends RecyclerView.Adapter<BluetoothDeviceAdapter.ViewHolder> {

    private final ArrayList<BluetoothDevice> devices;
    Context context;

    public BluetoothDeviceAdapter(Context context, ArrayList<BluetoothDevice> devices) {
        this.context = context;
        this.devices = devices;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.list_item_bluetooth_device, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String name = devices.get(position).getDeviceName();
        final String address = devices.get(position).getMacAddress();
        holder.nameTextView.setText(name);
        holder.addressTextView.setText(address);
        Log.d(context.getPackageName(), "Bind ViewHolder for device: " + devices.get(position).getDeviceName());
        holder.itemLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(context, address, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return devices.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        View itemLayout;
        TextView nameTextView, addressTextView;

        ViewHolder(View itemView) {
            super(itemView);
            itemLayout = itemView.findViewById(R.id.item_layout);
            nameTextView = itemView.findViewById(R.id.nameText);
            addressTextView = itemView.findViewById(R.id.addressView);
        }
    }
}
