package com.kyselov.iradio;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import com.crashlytics.android.Crashlytics;
import com.kyselov.iradio.models.RadioModel;
import com.kyselov.iradio.service.RadioPlayerService;
import com.un4seen.bass.BASS.DOWNLOADPROC;

import java.nio.ByteBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.un4seen.bass.BASS.BASS_ACTIVE_PAUSED;
import static com.un4seen.bass.BASS.BASS_ACTIVE_PLAYING;
import static com.un4seen.bass.BASS.BASS_ACTIVE_STOPPED;
import static com.un4seen.bass.BASS.BASS_ATTRIB_VOL;
import static com.un4seen.bass.BASS.BASS_CONFIG_ANDROID_AAUDIO;
import static com.un4seen.bass.BASS.BASS_CONFIG_ANDROID_SESSIONID;
import static com.un4seen.bass.BASS.BASS_CONFIG_NET_BUFFER;
import static com.un4seen.bass.BASS.BASS_CONFIG_NET_PLAYLIST;
import static com.un4seen.bass.BASS.BASS_CONFIG_NET_PREBUF_WAIT;
import static com.un4seen.bass.BASS.BASS_CONFIG_NET_READTIMEOUT;
import static com.un4seen.bass.BASS.BASS_CONFIG_NET_TIMEOUT;
import static com.un4seen.bass.BASS.BASS_CONFIG_UPDATETHREADS;
import static com.un4seen.bass.BASS.BASS_ChannelGetTags;
import static com.un4seen.bass.BASS.BASS_ChannelIsActive;
import static com.un4seen.bass.BASS.BASS_ChannelPause;
import static com.un4seen.bass.BASS.BASS_ChannelPlay;
import static com.un4seen.bass.BASS.BASS_ChannelSetAttribute;
import static com.un4seen.bass.BASS.BASS_ChannelSetSync;
import static com.un4seen.bass.BASS.BASS_ERROR_ALREADY;
import static com.un4seen.bass.BASS.BASS_ERROR_BUFLOST;
import static com.un4seen.bass.BASS.BASS_ERROR_CODEC;
import static com.un4seen.bass.BASS.BASS_ERROR_DECODE;
import static com.un4seen.bass.BASS.BASS_ERROR_ENDED;
import static com.un4seen.bass.BASS.BASS_ERROR_FILEFORM;
import static com.un4seen.bass.BASS.BASS_ERROR_FILEOPEN;
import static com.un4seen.bass.BASS.BASS_ERROR_FORMAT;
import static com.un4seen.bass.BASS.BASS_ERROR_HANDLE;
import static com.un4seen.bass.BASS.BASS_ERROR_ILLPARAM;
import static com.un4seen.bass.BASS.BASS_ERROR_ILLTYPE;
import static com.un4seen.bass.BASS.BASS_ERROR_INIT;
import static com.un4seen.bass.BASS.BASS_ERROR_MEM;
import static com.un4seen.bass.BASS.BASS_ERROR_NO3D;
import static com.un4seen.bass.BASS.BASS_ERROR_NOHW;
import static com.un4seen.bass.BASS.BASS_ERROR_NONET;
import static com.un4seen.bass.BASS.BASS_ERROR_NOPLAY;
import static com.un4seen.bass.BASS.BASS_ERROR_NOTAUDIO;
import static com.un4seen.bass.BASS.BASS_ERROR_NOTAVAIL;
import static com.un4seen.bass.BASS.BASS_ERROR_SPEAKER;
import static com.un4seen.bass.BASS.BASS_ERROR_SSL;
import static com.un4seen.bass.BASS.BASS_ERROR_START;
import static com.un4seen.bass.BASS.BASS_ERROR_TIMEOUT;
import static com.un4seen.bass.BASS.BASS_ERROR_UNKNOWN;
import static com.un4seen.bass.BASS.BASS_ERROR_UNSTREAMABLE;
import static com.un4seen.bass.BASS.BASS_ErrorGetCode;
import static com.un4seen.bass.BASS.BASS_FILEPOS_BUFFERING;
import static com.un4seen.bass.BASS.BASS_Free;
import static com.un4seen.bass.BASS.BASS_GetConfig;
import static com.un4seen.bass.BASS.BASS_Init;
import static com.un4seen.bass.BASS.BASS_OK;
import static com.un4seen.bass.BASS.BASS_PluginLoad;
import static com.un4seen.bass.BASS.BASS_STREAM_AUTOFREE;
import static com.un4seen.bass.BASS.BASS_STREAM_BLOCK;
import static com.un4seen.bass.BASS.BASS_STREAM_STATUS;
import static com.un4seen.bass.BASS.BASS_SYNC_END;
import static com.un4seen.bass.BASS.BASS_SYNC_FREE;
import static com.un4seen.bass.BASS.BASS_SYNC_META;
import static com.un4seen.bass.BASS.BASS_SYNC_MIXTIME;
import static com.un4seen.bass.BASS.BASS_SYNC_OGG_CHANGE;
import static com.un4seen.bass.BASS.BASS_SYNC_STALL;
import static com.un4seen.bass.BASS.BASS_SYNC_THREAD;
import static com.un4seen.bass.BASS.BASS_SetConfig;
import static com.un4seen.bass.BASS.BASS_StreamCreateURL;
import static com.un4seen.bass.BASS.BASS_StreamFree;
import static com.un4seen.bass.BASS.BASS_StreamGetFilePosition;
import static com.un4seen.bass.BASS.BASS_TAG_HTTP;
import static com.un4seen.bass.BASS.BASS_TAG_ICY;
import static com.un4seen.bass.BASS.BASS_TAG_META;
import static com.un4seen.bass.BASS.BASS_TAG_OGG;
import static com.un4seen.bass.BASS.SYNCPROC;

public class MediaController implements AudioManager.OnAudioFocusChangeListener {

    private RadioModel playingRadio;
    private ArrayList<RadioModel> playlist = new ArrayList<>();
    private static volatile MediaController Instance;

    private int requestBass; // request number/counter
    private int streamBass; // stream handle
    private int currentPlaylistNum;
    private volatile boolean download;
    private int countReconnected;

    //private static final int FLAGS_STREAM_CREATE_URL = BASS_STREAM_RESTRATE | BASS_STREAM_BLOCK | BASS_STREAM_STATUS | BASS_STREAM_AUTOFREE;
    private static final int FLAGS_STREAM_CREATE_URL = BASS_STREAM_BLOCK | BASS_STREAM_STATUS | BASS_STREAM_AUTOFREE;

    private int hasAudioFocus;

    private int audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
    private boolean resumeAudioOnFocusGain;

    private static final float VOLUME_DUCK = 0.2f;
    private static final float VOLUME_NORMAL = 1.0f;
    private static final int AUDIO_NO_FOCUS_NO_DUCK = 0;
    private static final int AUDIO_NO_FOCUS_CAN_DUCK = 1;
    private static final int AUDIO_FOCUSED = 2;
    private static final int MAX_RECONNECTED = 5;

    private static volatile DispatchQueue mediaQueue = new DispatchQueue("mediaQueue");

    private DispatchQueue syncStateQueue;

    private Runnable syncStateRunnable = new Runnable() {
        @Override
        public void run() {
            if (isPaused()) {
                stopRestartPlaying();
                AndroidUtilities.runOnUIThread(() -> {
                    EventCenter.get().postEventName(EventCenter.radioUpdateTitleAndArtist);
                    EventCenter.get().postEventName(EventCenter.radioPlayingPlayStateChanged);
                    EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, getPlayingRadio());
                });
                return;
            }

            if (isPlaying()) {
                // get the stream title
                DoMeta();
            }

            // monitor buffering progress
            if (isDownloading()) {
                AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.radioPlayingUpdateStreamPosition, getStreamPosition()));
                syncStateQueue.postRunnable(syncStateRunnable, 50);
            }
        }
    };

    public static MediaController get() {
        MediaController localInstance = Instance;
        if (localInstance == null) {
            synchronized (MediaController.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new MediaController();
                }
            }
        }
        return localInstance;
    }

    private MediaController() {

        syncStateQueue = new DispatchQueue("syncStateQueue");

        countReconnected = 1;

        initializationBass(); // init bass lib

        mediaQueue.postRunnable(() -> {
            if (BuildVars.BUILD_FOR_TV) return;

            try {
                final PhoneStateListener phoneStateListener = new PhoneStateListener() {
                    @Override
                    public void onCallStateChanged(final int state, String incomingNumber) {
                        AndroidUtilities.runOnUIThread(() -> {
                            if (state == TelephonyManager.CALL_STATE_RINGING) {
                                if (isPlaying() && !isPaused()) {
                                    pause();
                                }
                            }
                        });
                    }
                };

                final TelephonyManager mgr = (TelephonyManager) ApplicationLoader.applicationContext.getSystemService(Context.TELEPHONY_SERVICE);
                if (mgr != null) {
                    mgr.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
                }

            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
            if (isPlaying() && !isPaused()) {
                pause();
            }
            hasAudioFocus = 0;
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
        } else if (focusChange == AudioManager.AUDIOFOCUS_GAIN) {
            audioFocus = AUDIO_FOCUSED;
            if (resumeAudioOnFocusGain) {
                resumeAudioOnFocusGain = false;
                if (isPlaying() && isPaused()) {
                    play(playingRadio);
                }
            }
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            audioFocus = AUDIO_NO_FOCUS_CAN_DUCK;
        } else if (focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) {
            audioFocus = AUDIO_NO_FOCUS_NO_DUCK;
            if (isPlaying() && !isPaused()) {
                pause();
                resumeAudioOnFocusGain = true;
            }
        }

        setPlayerVolume();
    }

    private void setPlayerVolume() {
        if (streamBass == 0)
            return;

        float volume;

        if (audioFocus != AUDIO_NO_FOCUS_CAN_DUCK) {
            volume = VOLUME_NORMAL;
        } else {
            volume = VOLUME_DUCK;
        }

        BASS_ChannelSetAttribute(streamBass, BASS_ATTRIB_VOL, volume);
    }

    public void cleanup() {
        if (isRestartPlaying()) return;
        cleanupPlayer(true, true);
        BASS_Free();
        playlist.clear();
        FileLog.d("Destroy player");
    }

    public void cleanupPlayer(boolean notify, boolean stopService) {

        if (isRestartPlaying()) return;

        if (playingRadio != null) {
            playingRadio = null;
            if (notify) {
                NotificationsController.audioManager.abandonAudioFocus(this);
                hasAudioFocus = 0;
            }
        }

        if (stopService) {
            final Intent intent = new Intent(ApplicationLoader.applicationContext, RadioPlayerService.class);
            ApplicationLoader.applicationContext.stopService(intent);
        }
    }

    public RadioModel getPlayingRadio() {
        return playingRadio;
    }

    @SuppressWarnings("unused")
    public int getPlayingRadioNum() {
        return currentPlaylistNum;
    }

    @SuppressWarnings("WeakerAccess")
    public int getStreamPosition() {
        if (streamBass == 0) return 0;
        return 100 - (int) BASS_StreamGetFilePosition(streamBass, BASS_FILEPOS_BUFFERING);
    }

    public void setPlaylist(ArrayList<RadioModel> radios, RadioModel current) {
        if (playingRadio == current) {
            play(current);
            return;
        }

        playlist.clear();

        for (AtomicInteger a = new AtomicInteger(radios.size() - 1); a.get() >= 0; a.getAndDecrement()) {
            playlist.add(radios.get(a.get()));
        }

        currentPlaylistNum = playlist.indexOf(current);

        if (currentPlaylistNum == -1) {
            playlist.clear();
            currentPlaylistNum = playlist.size();
            playlist.add(current);
        }

        play(current);
    }

    public void playNext() {
        ArrayList<RadioModel> currentPlayList = playlist;

        currentPlaylistNum--;

        if (currentPlaylistNum < 0) {
            currentPlaylistNum = currentPlayList.size() - 1;
        }

        if (currentPlaylistNum < 0 || currentPlaylistNum >= currentPlayList.size()) {
            return;
        }

        RadioModel radio = currentPlayList.get(currentPlaylistNum);

        AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, radio));

        play(radio);
    }

    public void playPrevious() {
        ArrayList<RadioModel> currentPlayList = playlist;

        if (currentPlayList.isEmpty() || currentPlaylistNum < 0 || currentPlaylistNum >= currentPlayList.size()) {
            return;
        }

        currentPlaylistNum++;

        if (currentPlaylistNum >= currentPlayList.size()) {
            currentPlaylistNum = 0;
        }

        if (currentPlaylistNum < 0 || currentPlaylistNum >= currentPlayList.size()) {
            return;
        }

        RadioModel radio = currentPlayList.get(currentPlaylistNum);

        AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, radio));

        play(radio);
    }

    @SuppressWarnings("unused")
    public void findRadioInPlaylistAndPlay(RadioModel radio) {
        int index = playlist.indexOf(radio);
        if (index == -1) {
            play(radio);
        } else {
            playRadioAtIndex(index);
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void playRadioAtIndex(int index) {
        if (currentPlaylistNum < 0 || currentPlaylistNum >= playlist.size()) {
            return;
        }
        currentPlaylistNum = index;
        play(playlist.get(currentPlaylistNum));
    }

    private void checkAudioFocus() {
        int neededAudioFocus = 1;

        if (hasAudioFocus != neededAudioFocus) {
            hasAudioFocus = neededAudioFocus;

            int requestAudioFocus;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                final AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();
                final AudioFocusRequest focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                        .setAudioAttributes(audioAttributes)
                        .setAcceptsDelayedFocusGain(true)
                        .setOnAudioFocusChangeListener(this)
                        .build();
                requestAudioFocus = NotificationsController.audioManager.requestAudioFocus(focusRequest);
            } else {
                requestAudioFocus = NotificationsController.audioManager.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            }

            if (requestAudioFocus == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                audioFocus = AUDIO_FOCUSED;
            }
        }
    }

    private void initializationBass() {

        //BASS_DEVICE_AUDIOTRACK | BASS_DEVICE_FREQ

        if (BASS_Init(-1, 44100, 0)) {

            BASS_SetConfig(BASS_CONFIG_NET_PLAYLIST, 1); // enable playlist processing
            BASS_SetConfig(BASS_CONFIG_NET_PREBUF_WAIT, 0); // disable BASS_StreamCreateURL pre-buffering
            BASS_SetConfig(BASS_CONFIG_NET_BUFFER, 4000);
            BASS_SetConfig(BASS_CONFIG_NET_READTIMEOUT, 0);
            BASS_SetConfig(BASS_CONFIG_NET_TIMEOUT, 5000);
            BASS_SetConfig(BASS_CONFIG_UPDATETHREADS, 1);
            BASS_SetConfig(BASS_CONFIG_ANDROID_AAUDIO, 0);

            if (NotificationsController.audioManagerSessionID > 0)
                BASS_SetConfig(BASS_CONFIG_ANDROID_SESSIONID, NotificationsController.audioManagerSessionID);

            BASS_PluginLoad("libbass_aac.so", 0);
            BASS_PluginLoad("libbass_fx.so", 0);
            BASS_PluginLoad("libbass_ssl.so", 0);

            return;
        }

        Error("Can't initialize device");
    }

    private boolean preparePlaying(final RadioModel radio) {

        if (radio == null) {
            return false;
        }

        int requestBass;

        synchronized (MediaController.class) { // make sure only 1 thread at a time can do the following
            requestBass = ++this.requestBass; // increment the request counter for this request
        }

        BASS_StreamFree(streamBass); // close old stream
        //cleanupPlayer(false, false); // clean player

        int streamBass = BASS_StreamCreateURL(radio.getStream(), 0, FLAGS_STREAM_CREATE_URL, DownloadProc, requestBass); // open URL

        if (BASS_ErrorGetCode() == BASS_ERROR_INIT) {
            initializationBass();
            return false;
        }

        synchronized (MediaController.class) {
            if (requestBass != this.requestBass) { // there is a newer request, discard this stream
                if (streamBass != 0) cleanupPlayer(true, false);
                return false;
            }
            this.streamBass = streamBass; // this is now the current stream
        }

        if (this.streamBass == 0) {
            Error("Can't play the stream");
            return false;
        }

        playingRadio = radio;

        if (!isRestartPlaying()) {
            AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.radioUpdateTitleAndArtist));
        }

        // start prebuffer monitoring
        syncStateQueue.postRunnable(syncStateRunnable, 50);

        // set syncs for stream title updates
        BASS_ChannelSetSync(this.streamBass, BASS_SYNC_META | BASS_SYNC_MIXTIME | BASS_SYNC_THREAD, 0, MetaSync, this.requestBass); // Shoutcast
        BASS_ChannelSetSync(this.streamBass, BASS_SYNC_OGG_CHANGE | BASS_SYNC_THREAD, 0, MetaSync, this.requestBass); // Icecast/OGG
        // set sync for stalling/buffering
        BASS_ChannelSetSync(this.streamBass, BASS_SYNC_STALL | BASS_SYNC_MIXTIME | BASS_SYNC_THREAD, 0, StallSync, this.requestBass);
        // set sync for end of stream
        BASS_ChannelSetSync(this.streamBass, BASS_SYNC_END, 0, EndSync, this.requestBass);
        // set sync for free of stream
        BASS_ChannelSetSync(this.streamBass, BASS_SYNC_FREE | BASS_SYNC_MIXTIME | BASS_SYNC_THREAD, 0, FreeSync, this.requestBass);

        return true;
    }

    public void play(final RadioModel radio) {

        mediaQueue.postRunnable(() -> {

            if (!preparePlaying(radio)) {
                return;
            }

            // play it!
            if (BASS_ChannelPlay(streamBass, false)) {

                checkAudioFocus();
                setPlayerVolume();

                final Intent intent = new Intent(ApplicationLoader.applicationContext, RadioPlayerService.class);

                try {
                    ApplicationLoader.applicationContext.startService(intent);
                } catch (Throwable e) {
                    FileLog.e(e);
                }

                return;
            }

            Error("Can't play the stream");

            final Intent intent = new Intent(ApplicationLoader.applicationContext, RadioPlayerService.class);

            try {
                ApplicationLoader.applicationContext.stopService(intent);
            } catch (Throwable e) {
                FileLog.e(e);
            }
        });
    }

    private void Error(final String es) {

        if (isRestartPlaying()) return;

        // get error code in current thread for display in UI thread
        final int errorCode = BASS_ErrorGetCode();
        final AtomicReference<String> errorCodeMessage = new AtomicReference<>();

        switch (errorCode) {
            case BASS_ERROR_MEM:
                errorCodeMessage.set("Memory error");
                break;
            case BASS_ERROR_FILEOPEN:
                errorCodeMessage.set("Can't open the file");
                StreamInfo.get().setRadioStationMeta("Ошибка подключения...");
                break;
            case BASS_ERROR_HANDLE:
                errorCodeMessage.set("Handle is not a valid channel.");
                break;
            case BASS_ERROR_ILLTYPE:
                errorCodeMessage.set("An illegal type was specified.");
                break;
            case BASS_ERROR_ILLPARAM:
                errorCodeMessage.set("An illegal param was specified.");
                break;
            case BASS_ERROR_START:
                errorCodeMessage.set("The output is paused/stopped, use BASS_Start to start it.");
                break;
            case BASS_ERROR_DECODE:
                errorCodeMessage.set("The channel is not playable; it is a \"decoding channel\".");
                break;
            case BASS_ERROR_BUFLOST:
                errorCodeMessage.set("Should not happen... check that a valid window handle was used with BASS_Init.");
                break;
            case BASS_ERROR_NOHW:
                errorCodeMessage.set("No hardware voices are available (HCHANNEL only). This only occurs if the sample was loaded/created with the BASS_SAMPLE_VAM flag and BASS_VAM_HARDWARE is set in the sample's VAM mode, and there are no hardware voices available to play it.");
                break;
            case BASS_ERROR_TIMEOUT:
                errorCodeMessage.set("The server did not respond to the request within the timeout period, as set with the BASS_CONFIG_NET_TIMEOUT config option.");
                break;
            case BASS_ERROR_INIT:
                errorCodeMessage.set("BASS_Init has not been successfully called.");
                break;
            case BASS_ERROR_NOTAVAIL:
                errorCodeMessage.set("The BASS_STREAM_AUTOFREE flag cannot be combined with the BASS_STREAM_DECODE flag.");
                break;
            case BASS_ERROR_NONET:
                errorCodeMessage.set("No internet connection could be opened. Can be caused by a bad proxy setting.");
                break;
            case BASS_ERROR_SSL:
                errorCodeMessage.set("SSL/HTTPS support is not available. See BASS_CONFIG_LIBSSL.");
                break;
            case BASS_ERROR_FILEFORM:
                errorCodeMessage.set("The file's format is not recognised/supported.");
                break;
            case BASS_ERROR_UNSTREAMABLE:
                errorCodeMessage.set("The file cannot be streamed. This could be because an MP4 file's \"mdat\" atom comes before its \"moov\" atom.");
                break;
            case BASS_ERROR_NOTAUDIO:
                errorCodeMessage.set("The file does not contain audio, or it also contains video and videos are disabled.");
                break;
            case BASS_ERROR_CODEC:
                errorCodeMessage.set("The file uses a codec that is not available/supported. This can apply to WAV and AIFF files, and also MP3 files when using the \"MP3-free\" BASS version.");
                break;
            case BASS_ERROR_FORMAT:
                errorCodeMessage.set("The sample format is not supported by the device/drivers. If the stream is more than stereo or the BASS_SAMPLE_FLOAT flag is used, it could be that they are not supported.");
                break;
            case BASS_ERROR_SPEAKER:
                errorCodeMessage.set("The specified SPEAKER flags are invalid. The device/drivers do not support them, they are attempting to assign a stereo stream to a mono speaker or 3D functionality is enabled.");
                break;
            case BASS_ERROR_NO3D:
                errorCodeMessage.set("Could not initialize 3D support.");
                break;
            case BASS_ERROR_NOPLAY:
                errorCodeMessage.set("The channel is not playing (or handle is not a valid channel).");
                break;
            case BASS_ERROR_ALREADY:
                errorCodeMessage.set("The channel is already paused.");
                break;
            case BASS_ERROR_ENDED:
                errorCodeMessage.set("The channel has ended.");
                break;
            default:
                errorCodeMessage.set("Some other mystery problem!");
                break;
        }

        final String messageError = Utilities.format("%s\n(error code: %d)\n(error message: %s)", es, errorCode, errorCodeMessage.get());

        Crashlytics.log(messageError);
        FileLog.e(messageError);
        stopRestartPlaying();

        AndroidUtilities.runOnUIThread(() -> {

            EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, getPlayingRadio());
            EventCenter.get().postEventName(EventCenter.radioUpdateTitleAndArtist);
            EventCenter.get().postEventName(EventCenter.radioPlayingPlayStateChanged);
            EventCenter.get().postEventName(EventCenter.radioShowAlertError, messageError);

            if (errorCode == BASS_ERROR_FILEOPEN) {
                EventCenter.get().postEventName(EventCenter.restartPlayingError);
            }
        });
    }

    private boolean isRestartPlaying() {
        final int errorCode = BASS_ErrorGetCode();
        return (errorCode == BASS_ERROR_FILEOPEN
                || errorCode == BASS_OK
                || errorCode == BASS_ERROR_TIMEOUT
                || errorCode == BASS_ERROR_UNKNOWN) && countReconnected > 1 && countReconnected <= MAX_RECONNECTED;
    }

    private DOWNLOADPROC DownloadProc = (buffer, length, user) -> {
        if ((Integer) user != requestBass) return; // make sure this is still the current request

        if (buffer != null && length == 0) { // got HTTP/ICY tags
            try {

                CharsetDecoder dec = StandardCharsets.ISO_8859_1.newDecoder();
                ByteBuffer temp = ByteBuffer.allocate(buffer.limit()); // CharsetDecoder doesn't like a direct buffer?
                temp.put(buffer);
                temp.position(0);

                FileLog.d(dec.decode(temp).toString().replace("\0", "\n"));

            } catch (Exception ignored) {
            }
        }

        if (buffer == null && isPlaying()) {

            download = true;
            pause();

            mediaQueue.postRunnable(this.restartPlayingRunnable, 50);
        }
    };

    private void stopRestartPlaying() {

        if (countReconnected > 1) {
            mediaQueue.cancelRunnable(restartPlayingRunnable);
            countReconnected = 1;
        }
    }

    private Runnable restartPlayingRunnable = new Runnable() {

        @Override
        public void run() {

            if (countReconnected <= MAX_RECONNECTED) {

                final int localCountReconnected = countReconnected;
                FileLog.d("channel: RestartStream - " + localCountReconnected);
                StreamInfo.get().setRadioStationMeta(Utilities.format("Попытка подключения... %d", countReconnected));

                AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.restartPlaying));
                mediaQueue.postRunnable(restartPlayingRunnable, BASS_GetConfig(BASS_CONFIG_NET_TIMEOUT));

                play(playingRadio);
                countReconnected++;
            } else {
                FileLog.d("channel: RestartStream - error");
                stopRestartPlaying();
                AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.restartPlayingError));
            }
        }
    };

    private SYNCPROC EndSync = (handle, channel, data, user) -> {

        stopRestartPlaying();
        FileLog.d("channel: EndSync");
        cleanupPlayer(true, true);

        AndroidUtilities.runOnUIThread(() -> {
            EventCenter.get().postEventName(EventCenter.radioUpdateTitleAndArtist);
            EventCenter.get().postEventName(EventCenter.radioPlayingPlayStateChanged);
            EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, getPlayingRadio());
        });
    };

    private SYNCPROC FreeSync = (handle, channel, data, user) -> {

        if (isRestartPlaying()) return;
        FileLog.d("channel: FreeSync");
        NotificationsController.audioManager.abandonAudioFocus(this);
        BASS_StreamFree(streamBass); // close old stream
    };

    private SYNCPROC MetaSync = (handle, channel, data, user) -> DoMeta();

    private SYNCPROC StallSync = (int handle, int channel, int data, Object user) -> {
        download = data == 0;
        FileLog.d("StallSync: " + data);

        if (data == 0) {
            syncStateQueue.postRunnable(syncStateRunnable, 50);// start buffer monitoring
            AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, getPlayingRadio()));
        } else {
            stopRestartPlaying();
            AndroidUtilities.runOnUIThread(() -> {
                EventCenter.get().postEventName(EventCenter.radioPlayingPlayStateChanged);
                EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, getPlayingRadio());
            });
        }
    };

    private void DoMeta() {
        if (isRestartPlaying()) return;

        // get the broadcast name and URL
        String[] icy = (String[]) BASS_ChannelGetTags(streamBass, BASS_TAG_ICY);

        if (icy == null)
            icy = (String[]) BASS_ChannelGetTags(streamBass, BASS_TAG_HTTP); // no ICY tags, try HTTP

        if (icy != null) {
            for (String s : icy) {
                if (s.regionMatches(true, 0, "icy-name:", 0, 9)) {
                    StreamInfo.get().setRadioStationMeta(s.substring(9));
                }
            }
        }

        final AtomicReference<String> meta = new AtomicReference<>((String) BASS_ChannelGetTags(streamBass, BASS_TAG_META));
        if (meta.get() != null && meta.get().length() > 0) { // got Shoutcast metadata
            int ti = meta.get().indexOf("StreamTitle='");
            if (ti >= 0) {
                StreamInfo.get().setRadioStationMeta(meta.get().substring(ti + 13, meta.get().indexOf("';", ti + 13)));
            }
        } else {
            String[] ogg = (String[]) BASS_ChannelGetTags(streamBass, BASS_TAG_OGG);
            if (ogg != null) { // got Icecast/OGG tags
                AtomicReference<String> artist = new AtomicReference<>();
                AtomicReference<String> title = new AtomicReference<>();

                for (String s : ogg) {
                    if (s.regionMatches(true, 0, "artist=", 0, 7))
                        artist.set(s.substring(7));
                    else if (s.regionMatches(true, 0, "title=", 0, 6))
                        title.set(s.substring(6));
                }

                if (title.get() != null) {
                    if (artist.get() != null)
                        StreamInfo.get().setRadioStationMeta(artist + " - " + title);
                    else
                        StreamInfo.get().setRadioStationMeta(title.get());
                }

            }
        }

        AndroidUtilities.runOnUIThread(() -> EventCenter.get().postEventName(EventCenter.radioUpdateTitleAndArtist));
    }


    @SuppressWarnings("unused")
    public ArrayList<RadioModel> getPlaylist() {
        return playlist;
    }

    @SuppressWarnings("WeakerAccess")
    public boolean isPlaying() {
        if (streamBass == 0) return false;
        return BASS_ChannelIsActive(streamBass) == BASS_ACTIVE_PLAYING;
    }

    public boolean isPaused() {
        if (streamBass == 0) return false;

        switch (BASS_ChannelIsActive(streamBass)) {
            case BASS_ACTIVE_PAUSED:
            case BASS_ACTIVE_STOPPED:
                return true;
            default:
                return false;
        }
    }

    public void pause() {
        if (streamBass == 0) return;
        BASS_ChannelPause(streamBass);
        AndroidUtilities.runOnUIThread(() -> {
            EventCenter.get().postEventName(EventCenter.radioPlayingPlayStateChanged);
            EventCenter.get().postEventName(EventCenter.itemPlayingPlayStateChanged, getPlayingRadio());
        });
    }

    public boolean isDownloading() {
        return download;
    }
}
