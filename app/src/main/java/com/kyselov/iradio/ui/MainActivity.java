package com.kyselov.iradio.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.google.android.flexbox.JustifyContent;
import com.kyselov.iradio.BuildVars;
import com.kyselov.iradio.EventCenter;
import com.kyselov.iradio.MediaController;
import com.kyselov.iradio.R;
import com.kyselov.iradio.StreamInfo;
import com.kyselov.iradio.Utilities;
import com.kyselov.iradio.adapter.RadioUiAdapter;
import com.kyselov.iradio.models.RadioModel;

import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity implements EventCenter.EventCenterDelegate {

    @BindView(R.id.radioStation)
    RecyclerView mRecyclerView;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    ActionBar actionBar;

    private RadioUiAdapter radioUiAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setHomeAsUpIndicator(null);
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setDisplayUseLogoEnabled(true);
        }

        if (!BuildVars.BUILD_FOR_TV && Build.VERSION.SDK_INT >= 23) {
            if (!(checkSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED)) {
                requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, 6);
            }
        }

        final FlexboxLayoutManager layoutManager = new FlexboxLayoutManager(this);
        layoutManager.setJustifyContent(JustifyContent.SPACE_EVENLY);

        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(layoutManager);

        radioUiAdapter = new RadioUiAdapter(this);

        mRecyclerView.setAdapter(radioUiAdapter);
        radioUiAdapter.setOnItemClickListener((view, model, position) -> {

            final RadioModel playingRadio = MediaController.get().getPlayingRadio();

            if (playingRadio != null && playingRadio.getPrefix().equals(model.getPrefix()) && !MediaController.get().isPaused()) {
                MediaController.get().pause();
                return;
            }

            MediaController.get().setPlaylist(radioUiAdapter.getRadios(), model);
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        EventCenter.get().addObserver(this, EventCenter.radioPlayingUpdateStreamPosition);
        EventCenter.get().addObserver(this, EventCenter.radioUpdateTitleAndArtist);
        EventCenter.get().addObserver(this, EventCenter.radioShowAlertError);
        EventCenter.get().addObserver(this, EventCenter.radioUpdateCover);
        EventCenter.get().addObserver(this, EventCenter.restartPlaying);
        EventCenter.get().addObserver(this, EventCenter.restartPlayingError);
        EventCenter.get().addObserver(this, EventCenter.itemPlayingPlayStateChanged);

        EventCenter.get().postEventName(EventCenter.radioUpdateTitleAndArtist);
        EventCenter.get().postEventName(EventCenter.radioUpdateCover);
        EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, MediaController.get().getPlayingRadio());

        radioUiAdapter.startListening();
    }

    @Override
    protected void onStop() {
        EventCenter.get().removeObserver(this, EventCenter.radioPlayingUpdateStreamPosition);
        EventCenter.get().removeObserver(this, EventCenter.radioUpdateTitleAndArtist);
        EventCenter.get().removeObserver(this, EventCenter.radioUpdateCover);
        EventCenter.get().removeObserver(this, EventCenter.radioShowAlertError);
        EventCenter.get().removeObserver(this, EventCenter.restartPlaying);
        EventCenter.get().removeObserver(this, EventCenter.restartPlayingError);
        EventCenter.get().removeObserver(this, EventCenter.itemPlayingPlayStateChanged);

        radioUiAdapter.stopListening();

        super.onStop();
    }

    private Boolean exit = false;

    @Override
    public void onBackPressed() {
        if (!BuildVars.BUILD_FOR_TV) {
            super.onBackPressed();
            return;
        }

        if (exit) {
            MediaController.get().cleanup();
            super.onBackPressed();
        } else {

            Toast.makeText(this, "Press Back again to Exit.", Toast.LENGTH_SHORT).show();
            exit = true;

            Utilities.stageQueue.postRunnable(() -> {
                synchronized (MainActivity.class) {
                    exit = false;
                }
            }, 1500);
        }
    }


    @Override
    public void didReceivedEvent(int id, Object... args) {
        if (actionBar == null) return;

        if (id == EventCenter.radioUpdateCover) {
            final Bitmap cover = StreamInfo.get().getRadioStationCover();
            if (cover != null) {
                actionBar.setLogo(new BitmapDrawable(getResources(), cover));
            }
        } else if (id == EventCenter.radioShowAlertError) {
            final String msg = (String) args[0];
            if (!msg.isEmpty()) {
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(msg)
                        .setPositiveButton("OK", null)
                        .show();
            }
        } else if (id == EventCenter.restartPlaying
                || id == EventCenter.restartPlayingError
                || id == EventCenter.radioUpdateTitleAndArtist) {

            final String radioStation = StreamInfo.get().getRadioStation();
            final String radioStationMeta = StreamInfo.get().getRadioStationMeta();

            if (radioStation != null && !radioStation.isEmpty())
                actionBar.setTitle(radioStation);

            if (radioStationMeta != null && !radioStationMeta.isEmpty())
                actionBar.setSubtitle(radioStationMeta);

        } else if (id == EventCenter.radioPlayingUpdateStreamPosition) {
            final int streamPosition = (int) args[0];
            actionBar.setTitle(StreamInfo.get().getRadioStation());
            actionBar.setSubtitle(Utilities.format("Буферизация... %d%%", streamPosition));
        } else if (id == EventCenter.itemPlayingPlayStateChanged) {
            RadioModel radioModel = (RadioModel) args[0];
//            int state = (int) args[1];
            if (radioModel != null) {
                final int indexModelRadio = radioUiAdapter.getRadios().indexOf(radioModel);
                if (indexModelRadio > -1) {
                    ImageView f = mRecyclerView.findViewWithTag(radioModel.getPrefix()).findViewById(R.id.statePlayPause);
                    f.setImageResource(R.drawable.loading_animation3);
                   // radioUiAdapter.notifyItemChanged(indexModelRadio);
                }
            }
        }
    }
}
