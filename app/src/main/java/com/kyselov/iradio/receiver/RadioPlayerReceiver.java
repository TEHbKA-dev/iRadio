package com.kyselov.iradio.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;

import com.kyselov.iradio.MediaController;
import com.kyselov.iradio.service.RadioPlayerService;

public class RadioPlayerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) return;

        if (intent.getAction().equals(Intent.ACTION_MEDIA_BUTTON)) {
            if (intent.getExtras() == null) {
                return;
            }
            KeyEvent keyEvent = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
            if (keyEvent == null) {
                return;
            }
            if (keyEvent.getAction() != KeyEvent.ACTION_DOWN)
                return;

            switch (keyEvent.getKeyCode()) {
                case KeyEvent.KEYCODE_HEADSETHOOK:
                case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                    if (MediaController.get().isPaused()) {
                        MediaController.get().play(MediaController.get().getPlayingRadio());
                    } else {
                        MediaController.get().pause();
                    }
                    break;
                case KeyEvent.KEYCODE_MEDIA_PLAY:
                    MediaController.get().play(MediaController.get().getPlayingRadio());
                    break;
                case KeyEvent.KEYCODE_MEDIA_PAUSE:
                    MediaController.get().pause();
                    break;
                case KeyEvent.KEYCODE_MEDIA_STOP:
                    break;
                case KeyEvent.KEYCODE_MEDIA_NEXT:
                    MediaController.get().playNext();
                    break;
                case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                    MediaController.get().playPrevious();
                    break;
            }
        } else {
            if (intent.getAction().equals(RadioPlayerService.NOTIFY_PLAY)) {
                MediaController.get().play(MediaController.get().getPlayingRadio());
            } else if (intent.getAction().equals(RadioPlayerService.NOTIFY_PAUSE) ||
                    intent.getAction().equals(android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                MediaController.get().pause();
            } else if (intent.getAction().equals(RadioPlayerService.NOTIFY_NEXT)) {
                MediaController.get().playNext();
            } else if (intent.getAction().equals(RadioPlayerService.NOTIFY_CLOSE)) {
                MediaController.get().cleanupPlayer(true, true);
            } else if (intent.getAction().equals(RadioPlayerService.NOTIFY_PREVIOUS)) {
                MediaController.get().playPrevious();
            }
        }
    }
}
