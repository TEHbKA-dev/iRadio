package com.kyselov.iradio.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadata;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.IBinder;
import android.text.TextUtils;

import com.kyselov.iradio.AndroidUtilities;
import com.kyselov.iradio.ApplicationLoader;
import com.kyselov.iradio.MediaController;
import com.kyselov.iradio.FileLog;
import com.kyselov.iradio.EventCenter;
import com.kyselov.iradio.NotificationsController;
import com.kyselov.iradio.R;
import com.kyselov.iradio.StreamInfo;
import com.kyselov.iradio.models.RadioModel;
import com.kyselov.iradio.receiver.RadioPlayerReceiver;
import com.kyselov.iradio.ui.MainActivity;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

public class RadioPlayerService extends Service implements EventCenter.EventCenterDelegate {

    public static final String NOTIFY_PREVIOUS = "com.kyselov.iradio.android.radioplayer.previous";
    public static final String NOTIFY_CLOSE = "com.kyselov.iradio.android.radioplayer.close";
    public static final String NOTIFY_PAUSE = "com.kyselov.iradio.android.radioplayer.pause";
    public static final String NOTIFY_PLAY = "com.kyselov.iradio.android.radioplayer.play";
    public static final String NOTIFY_NEXT = "com.kyselov.iradio.android.radioplayer.next";

    private static final int ID_NOTIFICATION = 15;

    private MediaSession mediaSession;
    private PlaybackState.Builder playbackState;
    private Bitmap albumArtPlaceholder;

    private BroadcastReceiver headsetPlugReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY.equals(intent.getAction())) {
                MediaController.get().pause();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {

        FileLog.d("Create RadioPlayerService");

        EventCenter.get().addObserver(this, EventCenter.radioPlayingPlayStateChanged);

        mediaSession = new MediaSession(this, "iradioAudioPlayer");
        playbackState = new PlaybackState.Builder();
        albumArtPlaceholder = Bitmap.createBitmap(AndroidUtilities.dp(102), AndroidUtilities.dp(102), Bitmap.Config.ARGB_8888);

        final Drawable placeholder = getResources().getDrawable(R.drawable.nocover_big);
        placeholder.setBounds(0, 0, albumArtPlaceholder.getWidth(), albumArtPlaceholder.getHeight());
        placeholder.draw(new Canvas(albumArtPlaceholder));

        mediaSession.setCallback(new MediaSession.Callback() {
            @Override
            public void onPlay() {
                MediaController.get().play(MediaController.get().getPlayingRadio());
            }

            @Override
            public void onPause() {
                MediaController.get().pause();
            }

            @Override
            public void onSkipToNext() {
                MediaController.get().playNext();
            }

            @Override
            public void onSkipToPrevious() {
                MediaController.get().playPrevious();
            }

            @Override
            public void onSeekTo(long pos) {

            }

            @Override
            public void onStop() {
            }
        });

        mediaSession.setActive(true);

        registerReceiver(headsetPlugReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));

        super.onCreate();
    }

    @SuppressLint("NewApi")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        try {
            if (intent != null && (getPackageName() + ".STOP_PLAYER").equals(intent.getAction())) {
                FileLog.d("onStartCommand: STOP_PLAYER");
                MediaController.get().cleanupPlayer(true, true);
                return START_NOT_STICKY;
            }

            RadioModel playingRadio = MediaController.get().getPlayingRadio();
            if (playingRadio == null) {
                AndroidUtilities.runOnUIThread(this::stopSelf);
                return START_STICKY;
            }

            if(!TextUtils.isEmpty(AndroidUtilities.getSystemProperty("ro.miui.ui.version.code"))) {
                final ComponentName remoteComponentName = new ComponentName(getApplicationContext(), RadioPlayerReceiver.class.getName());
                try {
                    final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
                    mediaButtonIntent.setComponent(remoteComponentName);
                    final PendingIntent mediaPendingIntent = PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);
                    mediaSession.setMediaButtonReceiver(mediaPendingIntent);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }

            createNotification(playingRadio);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return START_STICKY;
    }

    @SuppressLint("NewApi")
    private void createNotification(RadioModel radio) {
        FileLog.d("Create notification");

        final Intent intent = new Intent(ApplicationLoader.applicationContext, MainActivity.class);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        final PendingIntent contentIntent = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, 0);
        Notification notification;
        final Bitmap[] albumArt = {null};
        Picasso.get().load(radio.getIconPng())
                .into(new Target() {
                    @Override
                    public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                        albumArt[0] = bitmap;
                    }

                    @Override
                    public void onBitmapFailed(Exception e, Drawable errorDrawable) {
                        albumArt[0] = albumArtPlaceholder;
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                        albumArt[0] = albumArtPlaceholder;
                    }
                });

        StreamInfo.get().setRadioStationCover(albumArt[0]);
        StreamInfo.get().setRadioStationPrefix(radio.getPrefix());
        StreamInfo.get().setRadioStation(radio.getTitle());
        EventCenter.get().postEventName(EventCenter.radioUpdateCover);

        boolean isPlaying = !MediaController.get().isPaused();

        PendingIntent pendingPrev = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_PREVIOUS).setComponent(new ComponentName(this, RadioPlayerReceiver.class)), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingStop = PendingIntent.getService(getApplicationContext(), 0, new Intent(this, getClass()).setAction(getPackageName() + ".STOP_PLAYER"), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingPlayPause = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(isPlaying ? NOTIFY_PAUSE : NOTIFY_PLAY).setComponent(new ComponentName(this, RadioPlayerReceiver.class)), PendingIntent.FLAG_CANCEL_CURRENT);
        PendingIntent pendingNext = PendingIntent.getBroadcast(getApplicationContext(), 0, new Intent(NOTIFY_NEXT).setComponent(new ComponentName(this, RadioPlayerReceiver.class)), PendingIntent.FLAG_CANCEL_CURRENT);

        final Notification.Builder builder = new Notification.Builder(this);
        builder.setSmallIcon(R.drawable.player)
                .setOngoing(isPlaying)
                .setContentTitle(radio.getTitle())
                .setContentText(MediaController.get().isDownloading() ? "Подключение" : (isPlaying ? "Играет": "Остановлено"))
                .setSubText(radio.getTitle())
                .setContentIntent(contentIntent)
                .setDeleteIntent(pendingStop)
                .setShowWhen(false)
                .setCategory(Notification.CATEGORY_TRANSPORT)
                .setPriority(Notification.PRIORITY_MAX)
                .setStyle(new Notification.MediaStyle()
                        .setMediaSession(mediaSession.getSessionToken())
                        .setShowActionsInCompactView(0, 1, 2));

        if (Build.VERSION.SDK_INT >= 26) {
            NotificationsController.checkOtherNotificationsChannel();
            builder.setChannelId(NotificationsController.OTHER_NOTIFICATIONS_CHANNEL);
        }

        if (albumArt[0] != null) {
            builder.setLargeIcon(albumArt[0]);
        } else {
            builder.setLargeIcon(albumArtPlaceholder);
        }

        if (MediaController.get().isDownloading()) {
            playbackState.setState(PlaybackState.STATE_BUFFERING, 0L, 1).setActions(0);
            builder.addAction(new Notification.Action.Builder(R.drawable.ic_action_previous, "", pendingPrev).build())
                    .addAction(new Notification.Action.Builder(R.drawable.loading_animation2, "", null).build())
                    .addAction(new Notification.Action.Builder(R.drawable.ic_action_next, "", pendingNext).build());
        } else {
            playbackState.setState(isPlaying ? PlaybackState.STATE_PLAYING : PlaybackState.STATE_PAUSED, 0L, isPlaying ? 1 : 0)
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE | PlaybackState.ACTION_PLAY | PlaybackState.ACTION_PAUSE | PlaybackState.ACTION_SKIP_TO_PREVIOUS | PlaybackState.ACTION_SKIP_TO_NEXT);
            builder.addAction(new Notification.Action.Builder(R.drawable.ic_action_previous, "", pendingPrev).build())
                    .addAction(new Notification.Action.Builder(isPlaying ? R.drawable.ic_action_pause : R.drawable.ic_action_play, "", pendingPlayPause).build())
                    .addAction(new Notification.Action.Builder(R.drawable.ic_action_next, "", pendingNext).build());
        }

        mediaSession.setPlaybackState(playbackState.build());
        final MediaMetadata.Builder meta = new MediaMetadata.Builder()
                .putBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART, albumArtPlaceholder)
                .putString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST, radio.getTitle())
                .putLong(MediaMetadata.METADATA_KEY_DURATION, 0L)
                //.putString(MediaMetadata.METADATA_KEY_TITLE, RadioInfo.get().getArtistAndTitle())
                .putString(MediaMetadata.METADATA_KEY_TITLE, MediaController.get().isDownloading() ? "Подключение" : (isPlaying ? "Играет": "Остановлено"))
                .putString(MediaMetadata.METADATA_KEY_ALBUM, radio.getTitle())
                ;

        mediaSession.setMetadata(meta.build());

        builder.setVisibility(Notification.VISIBILITY_PUBLIC);

        notification = builder.build();

        if (isPlaying) {
            startForeground(ID_NOTIFICATION, notification);
        } else {
            stopForeground(false);
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.notify(ID_NOTIFICATION, notification);
            }
        }
    }

    @SuppressLint("NewApi")
    @Override
    public void onDestroy() {
        FileLog.d("Destroy RadioPlayerService");
        unregisterReceiver(headsetPlugReceiver);
        super.onDestroy();
        mediaSession.release();
        EventCenter.get().removeObserver(this, EventCenter.radioPlayingPlayStateChanged);
    }

    @Override
    public void didReceivedEvent(int id, Object... args) {
        if (id == EventCenter.radioPlayingPlayStateChanged) {
            RadioModel radio = MediaController.get().getPlayingRadio();
            if (radio != null) {
                createNotification(radio);
            } else {
                stopSelf();
            }
        }
    }
}
