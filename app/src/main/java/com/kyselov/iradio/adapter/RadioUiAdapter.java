package com.kyselov.iradio.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.annotations.NotNull;
import com.kyselov.iradio.AndroidUtilities;
import com.kyselov.iradio.FileLog;
import com.kyselov.iradio.MediaController;
import com.kyselov.iradio.R;
import com.kyselov.iradio.StreamInfo;
import com.kyselov.iradio.models.RadioModel;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RadioUiAdapter extends RecyclerView.Adapter<RadioUiAdapter.RadioHolder> {

    static {
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);
    }

    private final MyChildEventListener mChildEventListener = new MyChildEventListener();
    private DatabaseReference mDatabaseReference;
    private Context mContext;
    private ArrayList<String> mRadioIds = new ArrayList<>();
    private ArrayList<RadioModel> mRadios = new ArrayList<>();
    private OnItemClickListener mOnItemClickListener;

    private int currentlyPlayingPosition = 0;

    public RadioUiAdapter(final Context context) {

        mContext = context;
        mDatabaseReference = FirebaseDatabase.getInstance().getReference().child("radio");
    }

    public void setOnItemClickListener(final OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    @NonNull
    @Override
    public RadioHolder onCreateViewHolder(@NotNull ViewGroup parent, int viewType) {
        return new RadioHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_new_radio, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RadioHolder holder, int position) {
        holder.bind(mRadios.get(position));
    }

    @Override
    public int getItemCount() {
        return mRadios.size();
    }

    public ArrayList<RadioModel> getRadios() {
        return mRadios;
    }

    public void stopListening() {
        mDatabaseReference.removeEventListener(mChildEventListener);
    }

    public void startListening() {
        if (mDatabaseReference != null) {
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    public interface OnItemClickListener {
        void onItemClick(final View view, final RadioModel model, int position);
    }

    class MyChildEventListener implements ChildEventListener {

        @Override
        public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            if (mRadioIds.indexOf(dataSnapshot.getKey()) == -1) {
                FileLog.d("onChildAdded:" + dataSnapshot.getKey());

                // A new radio has been added, add it to the displayed list
                final RadioModel radio = dataSnapshot.getValue(RadioModel.class);

                mRadioIds.add(dataSnapshot.getKey());
                mRadios.add(radio);
                notifyItemInserted(mRadios.size() - 1);
            }
        }

        @Override
        public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            FileLog.d("onChildChanged:" + dataSnapshot.getKey());

            final RadioModel newRadio = dataSnapshot.getValue(RadioModel.class);
            final String radioKey = dataSnapshot.getKey();
            final int radioIndex = mRadioIds.indexOf(radioKey);

            if (radioIndex > -1) {
                mRadios.set(radioIndex, newRadio);

                // Update the RecyclerView
                notifyItemChanged(radioIndex);
            } else {
                FileLog.d("onChildChanged:unknown_child:" + radioKey);
            }
        }

        @Override
        public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
            FileLog.d("onChildRemoved:" + dataSnapshot.getKey());

            // A radio has changed, use the key to determine if we are displaying this
            // radio and if so remove it.
            final String radioKey = dataSnapshot.getKey();

            // [START_EXCLUDE]
            int radioIndex = mRadioIds.indexOf(radioKey);

            if (radioIndex > -1) {

                // Remove data from the list
                mRadioIds.remove(radioIndex);
                mRadios.remove(radioIndex);

                // Update the RecyclerView
                notifyItemRemoved(radioIndex);

            } else {
                FileLog.d("onChildRemoved:unknown_child:" + radioKey);
            }
            // [END_EXCLUDE]
        }

        @Override
        public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
            FileLog.d("onChildMoved:" + dataSnapshot.getKey());
            //final Radio movedRadio = dataSnapshot.getValue(Radio.class);
            //String radioKey = dataSnapshot.getKey();
        }

        @Override
        public void onCancelled(@NonNull DatabaseError databaseError) {
            FileLog.e("radioList:onCancelled", databaseError.toException());
            Toast.makeText(mContext, "Failed to load comments.", Toast.LENGTH_LONG).show();
        }
    }

    class RadioHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.radioStation)
        TextView radioStation;

        @BindView(R.id.coverTrack)
        ImageView coverTrack;

        @BindView(R.id.statePlayPause)
        ImageView statePlayPause;

        RadioHolder(@NonNull View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bind(@NonNull RadioModel radio) {
            itemView.setTag(radio.getPrefix());

            setName(radio.getTitle());
            setCoverTrack(radio.getIconPng());
            updateStatePlayPause();

            if (mOnItemClickListener != null) {
                itemView.setOnClickListener(v -> {

                    itemView.setSelected(true);
                    statePlayPause.setImageResource(R.drawable.loading_animation3);
                    mOnItemClickListener.onItemClick(itemView, radio, getAdapterPosition());
                    //if (currentlyPlayingPosition != getAdapterPosition())
                    //    notifyItemChanged(currentlyPlayingPosition);
                    currentlyPlayingPosition = getAdapterPosition();
                });
            }
        }

        void updateStatePlayPause() {
            final RadioModel radioModel = mRadios.get(getAdapterPosition());

            if (radioModel.getPrefix().equals(StreamInfo.get().getRadioStationPrefix())) {
                if (!MediaController.get().isPaused())
                    statePlayPause.setImageResource(R.drawable.ic_pause_circle_filled_black_24dp);
                else
                    statePlayPause.setImageResource(R.drawable.ic_play_circle_filled_black_24dp);
                itemView.setSelected(true);
            } else {
                statePlayPause.setImageDrawable(null);
                itemView.setSelected(false);
            }
        }

        void setName(@Nullable String name) {
            radioStation.setText(name);
        }

        void setCoverTrack(String url) {

            /*ImageView.ScaleType curr = coverTrack.getScaleType();
            ImageView.ScaleType[] all = ImageView.ScaleType.values();
            int nextOrdinal = (curr.ordinal() + 1) % all.length;
            ImageView.ScaleType next = all[nextOrdinal];
            coverTrack.setScaleType(next);*/

            Picasso.get()
                    .load(url)
                    .placeholder(AndroidUtilities.ProgressDrawable())
                    .error(R.drawable.nocover_big)
                    .into(coverTrack);
        }
    }
}
