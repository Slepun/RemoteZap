package com.biotronics.remotezap;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class ThpMgmRecycleAdapter extends RecyclerView.Adapter<ThpMgmRecycleAdapter.RecViewHolder> {

    private List<Therapy> therapies;
    private List<Therapy> therapiesBk;
    private Context context;
    private final ClickListener listener;
    private DeviceStatus mDevStatGet;

    public ThpMgmRecycleAdapter(Context context, List<Therapy> therapies, ClickListener listener) {
            this.therapies = therapies;
        //noinspection Convert2Diamond
        therapiesBk = new ArrayList<Therapy>(50);
            therapiesBk.addAll(therapies);
            this.context = context;
            this.listener = listener;
            mDevStatGet = DeviceStatus.getInstance();
    }

    @NonNull
    @Override
    public RecViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater
                .from(viewGroup.getContext())
                .inflate(R.layout.item_thp_mgm_recview, viewGroup, false);
        return new RecViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(RecViewHolder holder, int position) {
        Therapy therapy = therapies.get(position);

        holder.bind(therapy);

        holder.itemView.setOnClickListener(view -> {
            boolean expanded = therapy.isExpanded();
            therapy.setExpanded(!expanded);
            notifyItemChanged(position);
        });

        holder.settingsButton.setOnClickListener(view ->
        {
            //creating a popup menu
            PopupMenu popup = new PopupMenu(view.getContext(), holder.settingsButton);
            //inflating menu from xml resource
            popup.inflate(R.menu.thp_settings);
            //adding click listener
            popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.thp_edit:
                            //handle menu1 click
                            //Toast.makeText(view.getContext(), "mENU ", Toast.LENGTH_SHORT).show();
                            Toast.makeText(view.getContext(), "TODO Edit" +
                                    String.valueOf(holder.getAdapterPosition()), Toast.LENGTH_SHORT).show();
                            //TODO EDIT
                            return true;
                        case R.id.thp_remove:
                            //handle menu2 click
                            Toast.makeText(view.getContext(), "TODO Remove " +
                                    String.valueOf(holder.getAdapterPosition()), Toast.LENGTH_SHORT).show();
                        default:
                            return false;
                    }
                }
            });
            popup.show();
        });
    }

    void updateData(List<Therapy> newThps)
    {
        therapies.clear();
        therapiesBk.clear();
        therapies.addAll(newThps);
        therapiesBk.addAll(therapies);
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount(){
        return therapies == null ? 0 : therapies.size();
    }

    void filterOutDevices(int devId)
    {
        if(therapiesBk.isEmpty())
        {
            therapiesBk.addAll(therapies);
        }
        if (devId <= Therapy.MAX_NUM_OF_DEV) {
            therapies.clear();
            if (devId == 0)
            {
                therapies.addAll(therapiesBk);
            } else{
                for (Therapy thp : therapiesBk)
                    if (thp.containsId(devId)){
                        therapies.add(thp);
                    }
            }
        }
        notifyDataSetChanged();
    }

    public class RecViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

        private TextView title;
        private TextView device;
        private TextView shortDesc;
        ProgressBar progresUpload;
        private Button btnUploadThp;
        private TextView settingsButton;
        private WeakReference<ClickListener> listenerRef;

        public RecViewHolder(View itemView) {
            super(itemView);

            title = itemView.findViewById(R.id.item_thp_title);
            device = itemView.findViewById(R.id.item_thp_device);
            shortDesc = itemView.findViewById(R.id.item_thp_description_short);
            btnUploadThp = itemView.findViewById(R.id.item_thp_uploadThp);
            progresUpload = itemView.findViewById(R.id.progressBarSendThp);
            settingsButton = itemView.findViewById(R.id.item_thp_settings);
        }

        RecViewHolder(View itemView, ClickListener listener) {
            super(itemView);

            listenerRef = new WeakReference<>(listener);
            title = itemView.findViewById(R.id.item_thp_title);
            device = itemView.findViewById(R.id.item_thp_device);
            shortDesc = itemView.findViewById(R.id.item_thp_description_short);
            btnUploadThp = itemView.findViewById(R.id.item_thp_uploadThp);
            btnUploadThp.setOnClickListener(this);
            progresUpload = itemView.findViewById(R.id.progressBarSendThp);
            settingsButton = itemView.findViewById(R.id.item_thp_settings);
        }

        private void bind(Therapy therapy) {
            boolean expanded = therapy.isExpanded();
            btnUploadThp.setEnabled(mDevStatGet.getIsSerialPortOpen());
            if(expanded)
            {
                //subItem.setVisibility(View.VISIBLE);
                shortDesc.setEllipsize(null);
                shortDesc.setSingleLine(false);
                progresUpload.setVisibility(View.VISIBLE);
                btnUploadThp.setVisibility(View.VISIBLE);
            }
            else
            {
                //subItem.setVisibility(View.GONE);
                shortDesc.setEllipsize(TextUtils.TruncateAt.END);
                shortDesc.setSingleLine(true);
                progresUpload.setVisibility(View.GONE);
                btnUploadThp.setVisibility(View.GONE);
            }

            title.setText(therapy.getTitle());
            device.setText(context.getResources().getString(R.string.thpdevice) + therapy.getDevice());
            shortDesc.setText(therapy.getDescription());
            progresUpload.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onClick(View view) {
            if (view.getId() == btnUploadThp.getId()) {
                Toast.makeText(view.getContext(), "ITEM PRESSED = " +
                        String.valueOf(getAdapterPosition()), Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(view.getContext(), "ROW PRESSED = " + String.valueOf(getAdapterPosition()), Toast.LENGTH_SHORT).show();
            }
            listenerRef.get().onPositionClicked(therapies.get(getAdapterPosition()).getThpId(), getAdapterPosition());
        }
    }
}
