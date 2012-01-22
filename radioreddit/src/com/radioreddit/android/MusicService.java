/*
 * This file is part of radio reddit for Android.
 *
 * radio reddit for Android is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * radio reddit for Android is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with radio reddit for Android.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.radioreddit.android;

import java.io.IOException;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class MusicService extends Service {
    private static final int NOTIFICATION = R.string.playing_music;
    
    private final IBinder mBinder = new MusicBinder();
    private MediaPlayer mMediaPlayer = null;
    private MainActivity mMainActivity = null;
    private String mStreamUrl = null;
    private boolean mResumeAfterCall = false;
    
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                // Stop the stream if playing and the ringer isn't in silent/vibrate
                if (isPlaying() && ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) > 0) {
                    stop();
                    mResumeAfterCall = true;
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                // Stop stream if playing
                if (isPlaying()) {
                    stop();
                    mResumeAfterCall = true;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                // Resume playing stream if it was playing before
                if (mResumeAfterCall) {
                    play();
                    mResumeAfterCall = false;
                }
                break;
            }
        }
    };
    
    @Override
    public void onCreate() {
        // Register our state listener with the system
        TelephonyManager telemgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telemgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        // Remove any reference to the main activity
        detatch();
        
        // Remove any reference to the media player
        stop();
        
        // Unregister our state listener from the system 
        TelephonyManager telemgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telemgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    // Called by the main activity after it has connected to this service. Lets
    // us to display toasts, update the progress bar, and notify of song change
    // from here
    public void attach(MainActivity mainActivity) {
        mMainActivity = mainActivity;
    }
    
    // Called by the main activity when it is paused, stopped, or destroyed. So
    // that we don't try to use something that's not there
    public void detatch() {
        mMainActivity = null;
    }
    
    // Only called by the phone state listener if we need to resume play
    private void play() {
        if (mStreamUrl != null) {
            play(mStreamUrl);
        }
    }
    
    // Called by the main activity when we need to play a new stream. Can be
    // called when stopped or already playing
    public void play(String streamUrl) {
        if (isPlaying()) {
            stop();
        }
        try {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setDataSource(streamUrl);
            mMediaPlayer.prepareAsync();
            showNotification();
            mStreamUrl = streamUrl;
        } catch (IOException e) {
            Log.e("music service - radio reddit", "IOException while trying to start media player", e);
            // try to display error message via attached activity
            if (mMainActivity != null) {
                mMainActivity.toast(R.string.network_error);
            }
        }
    }
    
    // Called by the main activity when we should stop playing. Can be called
    // when playing or already stopped
    public void stop() {
        if (isPlaying()) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            hideNotification();
        }
    }
    
    // Used in this class as well as in the main activity to determine if we
    // are currently playing music
    public boolean isPlaying() {
        return mMediaPlayer != null;
    }
    
    public int getDuration() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getDuration();
        }
        return -1;
    }
    
    public int getCurrentPosition() {
        if (mMediaPlayer != null) {
            return mMediaPlayer.getCurrentPosition();
        }
        return -1;
    }
    
    /**
     * Not only displays a notification that music is playing but also sets the
     * service to be in the foreground so that it is not stopped automatically
     * when no activities are connected. The current implementation only works
     * on API level 5+ but there is info at:
     * http://developer.android.com/reference/android/app/Service.html#startForeground%28int,%20android.app.Notification%29
     * regarding making this work on earlier API versions
     */
    private void showNotification() {
        final CharSequence text = getText(NOTIFICATION);
        Notification notification = new Notification(R.drawable.ic_home, text, System.currentTimeMillis());
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        notification.setLatestEventInfo(this, text, "", contentIntent);
        startForeground(NOTIFICATION, notification);
    }
    
    /**
     * Not only removes the notification (as directed by the 'true' argument)
     * but also sets the service to run in the background again
     */
    private void hideNotification() {
        stopForeground(true);
    }
    
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    
    // Used by the media player for acting when it is prepared
    // Should just start the music playback
    private final MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
        }
    };
}
