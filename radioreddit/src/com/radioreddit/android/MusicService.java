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
import java.util.ArrayList;
import java.util.Iterator;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.radioreddit.android.api.GetStationStatus;
import com.radioreddit.android.api.RedditApi;
import com.radioreddit.android.api.Relay;
import com.radioreddit.android.api.StationStatus;
import com.radioreddit.android.api.Stream;

public class MusicService extends Service {
    public static final boolean DEBUG = false;
    private static final String TAG = "MusicService";
    
    private static final int NOTIFICATION = R.string.playing_music;
    
    // TODO: Figure out how to load all this information when the app starts up
    private static final Stream[] STREAMS = {
        new Stream("Main", "/api/"),
        new Stream("Electronic", "/api/electronic/"),
        new Stream("Rock", "/api/rock/"),
        new Stream("Metal", "/api/metal/"),
        new Stream("Indie", "/api/indie/"),
        new Stream("Hip Hop", "/api/hiphop/"),
        new Stream("Random", "/api/random/"),
        new Stream("Talk", "/api/talk/"),
    };
    
    public static final CharSequence[] STREAM_NAMES = {
        "Main",
        "Electronic",
        "Rock",
        "Metal",
        "Indie",
        "Hip Hop",
        "Random",
        "Talk"
    };
    
    // App intents use fully qualified name because it makes integration with
    //  the appwidget receiver's intent filter in the manifest easier
    public static final String ACTION_SONG_INFO_CHANGED = "com.radioreddit.android.MusicService.ACTION_SONG_INFO_CHANGED";
    public static final String ACTION_STATION_CHANGED = "com.radioreddit.android.MusicService.ACTION_STATION_CHANGED";
    public static final String ACTION_PLAYER_COMMAND = "com.radioreddit.android.MusicService.ACTION_PLAYER_COMMAND";
    public static final String ACTION_REQUEST_UPDATE = "com.radioreddit.android.MusicService.ACTION_REQUEST_UPDATE";
    public static final String ACTION_UPDATE_WIDGET = "com.radioreddit.android.MusicService.ACTION_UPDATE_WIDGET";
    // Keys used to passing and retrieving appropriate information with intent extras
    public static final String KEY_SONG_INFO = "key_song_info";
    public static final String KEY_STREAM_NAME = "key_stream_name";
    public static final String KEY_COMMAND = "key_player_command";
    public static final String KEY_IS_PLAYING = "key_is_playing";
    // Values for commands captured by the service
    public static final int CMD_INVALID = 0;
    public static final int CMD_UPVOTE = 1;
    public static final int CMD_DOWNVOTE = 2;
    public static final int CMD_TOGGLE_PLAY = 3;
    public static final int CMD_SAVE = 4;
    public static final int CMD_CHANGE_STREAM = 5;
    // Session data used to store login after service has been shutdown
    public static final String PREF_USER = "user";
    public static final String PREF_MODHASH = "modhash";
    public static final String PREF_COOKIE = "cookie";
    public static final String PREF_STREAM = "stream";
    // Time delay in milliseconds between attempts to grab songinfo
    private static final long UPDATE_INTERVAL = 15000;
    
    private Context mContext;
    private SharedPreferences mPreferences = null;
    private Notification mNotification = null;
    private Stream mStream = null;
    private AllSongInfo mSongInfo = null;
    
    // Update runnable fires at a specified interval to grab new songinfo from the server
    private Runnable mUpdater = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) {
                Log.d(TAG, "++Try Song Info Request++");
            }
            RedditApi.requestSongInfo(MusicService.this, getCookie(),
                    "http://www.radioreddit.com" + mStream.status + "status.json");
            mHandler.postDelayed(mUpdater, UPDATE_INTERVAL);
        }
    };
    
    private Handler mHandler = new Handler();
    private ArrayList<PlaystateChangedListener> mListeners = new ArrayList<PlaystateChangedListener>(1);
    private PlaystateChangedListener mWidgetPlaystateListener = new PlaystateChangedListener() {
        @Override
        public void onPlaystateChanged(boolean isPlaying) {
            final Intent widgetUpdate = new Intent(MusicService.ACTION_UPDATE_WIDGET);
            final Bundle extras = new Bundle();
            extras.putParcelable(KEY_SONG_INFO, mSongInfo);
            extras.putString(KEY_STREAM_NAME, mStream.name);
            extras.putBoolean(KEY_IS_PLAYING, isPlaying);
            widgetUpdate.putExtras(extras);
            mContext.sendBroadcast(widgetUpdate);
        }
    };
    
    private final IBinder mBinder = new MusicBinder();
    private MediaPlayer mMediaPlayer = null;
    private String mStreamUrl = null;
    private boolean mCanPlay = true;
    private boolean mResume = false;
    
    private final PhoneStateListener mPhoneStateListener = new PhoneStateListener() {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {
            switch (state) {
            case TelephonyManager.CALL_STATE_RINGING:
                mCanPlay = false;
                // Stop the stream if playing and the ringer isn't in silent/vibrate
                if (isPlaying() && ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).getStreamVolume(AudioManager.STREAM_RING) > 0) {
                    stop();
                    mResume = true;
                }
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:
                mCanPlay = false;
                // Stop stream if playing
                if (isPlaying()) {
                    stop();
                    mResume = true;
                }
                break;
            case TelephonyManager.CALL_STATE_IDLE:
                mCanPlay = true;
                // Resume playing stream if it was playing before and we have Internet access now
                if (mResume) {
                    final ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                    if (connectivityManager.getActiveNetworkInfo() != null) {
                        // We have an Internet connection so just start playing
                        play();
                        mResume = false;
                    } else {
                        // We're probably waiting for the data to come back so
                        // let's wait for the data connection state change
                        // listener to start playing instead
                    }
                }
                break;
            }
        }
        
        @Override
        public void onDataConnectionStateChanged(final int state) {
            if (mResume && mCanPlay && state == TelephonyManager.DATA_CONNECTED) {
                play();
                mResume = false;
            }
        }
    };
    
    // Receives command intent and update request intent. Grabs command from
    //  intent and passes it down for processing, if no command is specified
    //  then it supplies an invalid value. Update request intents are all
    //  called from a sendOrderedBroadcast so they need to be treated differently
    BroadcastReceiver mCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(ACTION_PLAYER_COMMAND)) {
                if (DEBUG) {
                    Log.d(TAG, "++Command Intent Received++");
                }
                // The intent is sending a command
                processCommand(intent.getIntExtra(KEY_COMMAND, CMD_INVALID), intent);
            } else if (action.equals(ACTION_REQUEST_UPDATE)) {
                // Loopback intent is always a result of a sendOrderedBroadcast
                //  so merely add the appropriate info to the intent and it'll
                //  move itself along
                if (DEBUG) {
                    Log.d(TAG, "++Update Request Intent Received++");
                }
                final Bundle bundle = getResultExtras(true);
                bundle.putParcelable(KEY_SONG_INFO, mSongInfo);
                bundle.putString(KEY_STREAM_NAME, mStream.name);
                bundle.putBoolean(KEY_IS_PLAYING, isPlaying());
                setResult(Activity.RESULT_OK, null, bundle);
            }
        }
    };
    
    @Override
    public void onCreate() {
        if (DEBUG) {
            Log.d(TAG, "++OnCreate++");
        }
        
        mContext = getApplicationContext();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        
        // Register a receiver to grab any commands or update requests sent
        //  from widgets and/or activities
        final IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_PLAYER_COMMAND);
        filter.addAction(ACTION_REQUEST_UPDATE);
        registerReceiver(mCommandReceiver, filter);
        
        // Grab any saved stream and if one was never saved default to Main
        changeStream(mPreferences.getInt(PREF_STREAM, 0));
        
        // Drop some filler data into the songinfo until it can be loaded
        clearSongInfo();
        
        // Set the playchangedlistener for the widgets
        registerPlaystateListener(mWidgetPlaystateListener);
        
        // Register our state listener with the system
        final TelephonyManager telemgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telemgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE | PhoneStateListener.LISTEN_DATA_CONNECTION_STATE);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) {
            Log.d(TAG, "++OnStartCommand++");
        }
        
        // Grab any commands that may be launched with the service, usually
        //  happens with widgets activating the service
        if (intent != null && intent.getAction() != null) {
            final String action = intent.getAction();
            if (action.equals(ACTION_PLAYER_COMMAND)) {
                processCommand(intent.getIntExtra(KEY_COMMAND, CMD_INVALID), intent);
            }
        }
        
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "++OnDestroy++");
        }
        
        // Remove any reference to the media player
        stop();
        
        // Unregister widget playstate listener
        unregisterPlaystateListener(mWidgetPlaystateListener);
        
        // Unregister the command receiver
        unregisterReceiver(mCommandReceiver);
        
        // Unregister our state listener from the system 
        final TelephonyManager telemgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telemgr.listen(mPhoneStateListener, PhoneStateListener.LISTEN_NONE);
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    
    class GrabStationStatus extends GetStationStatus{

		public GrabStationStatus() {
			super(MusicService.this, null);
		}

		@Override
		protected void onPostExecute(StationStatus result) {
			if(result == null){
				return;
			}
			
			mStreamUrl = result.relay;
			play();
		}
    	
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
            stopTimer();
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
            mMediaPlayer.setOnPreparedListener(mPreparedListener);
            mMediaPlayer.setDataSource(streamUrl);
            mMediaPlayer.prepareAsync();
            showNotification();
            mStreamUrl = streamUrl;
        } catch (IOException e) {
            Log.e(TAG, "IOException while trying to start media player", e);
            toast(R.string.network_error);
        }
    }
    
    // Called by the main activity when we should stop playing. Can be called
    // when playing or already stopped
    public void stop() {
        if (isPlaying()) {
            stopTimer();
            
            // Tell any frontend activities or widgets that the service has
            // stopped playing and they should update their buttons
            Iterator<PlaystateChangedListener> listeners = mListeners.iterator();
            while (listeners.hasNext()) {
                listeners.next().onPlaystateChanged(false);
            }
            
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
            hideNotification();
        }
    }
    
    // Used in this class as well as in the main activity to determine if we
    // are currently playing music
    public boolean isPlaying() {
        return mMediaPlayer != null /* && mMediaPlayer.isPlaying() */;
    }
    
    private void processCommand(int command, Intent intent) {
        switch (command) {
        case CMD_UPVOTE:
            toggleUpvote();
            break;
        case CMD_DOWNVOTE:
            toggleDownvote();
            break;
        case CMD_SAVE:
            toggleSave();
            break;
        case CMD_TOGGLE_PLAY:
            if (isPlaying()) {
                stop();
            } else {
            	new GrabStationStatus().execute("http://www.radioreddit.com" + mStream.status + "status.json");
            }
            break;
        case CMD_CHANGE_STREAM:
            int streamId = intent.getIntExtra("stream_id", 0);
            changeStream(streamId);
            break;
        default:
            Log.e(TAG, "invalid command received");
            break;
        }
    }
    
    private void changeStream(int streamId) {
        if (DEBUG) {
            Log.d(TAG, "++ChangeStream++");
        }
        
        // Set the current station and begin playback if already playing
        mStream = STREAMS[streamId];
        if (isPlaying()) {
        	new GrabStationStatus().execute("http://www.radioreddit.com" + mStream.status + "status.json");
        }
        
        // Save streamId for future reference
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putInt(PREF_STREAM, streamId);
        editor.commit();
        
        // Inform user the stream has been successfully changed
        //toast(getString(R.string.now_tuned_to) + " " + mStream.name);
        
        // Change the notification title to the current stream
        // and drop in some temporary text, will get update when
        // OnSongInfoChanged successfully completes
        updateNotification(getString(R.string.loading_notification), "", mStream.name);
        
        updateWidget();
        
        // Broadcast change to any widgets and/or activities.
        final Intent informStationChg = new Intent(ACTION_STATION_CHANGED);
        informStationChg.putExtra(KEY_STREAM_NAME, mStream.name);
        sendBroadcast(informStationChg);
    }
    
    // getSongInfo has provided new info. Intent used to update the
    // displays on any widgets and/or activies
    public void onSongInfoChanged(AllSongInfo song) {
        if (DEBUG) {
            Log.d(TAG, "++OnSongChanged++");
        }
        mSongInfo = song;
        
        if (!isPlaying()) {
            return;
        }
        
        // Update the notification
        updateNotification(mSongInfo.artist, mSongInfo.title, mStream.name);
        updateWidget();
        
        // Send the entire songinfo object
        Intent intent = new Intent(ACTION_SONG_INFO_CHANGED);
        intent.putExtra(KEY_SONG_INFO, mSongInfo);
        mContext.sendBroadcast(intent);
    }
    
    private void startTimer() {
        mHandler.post(mUpdater);
    }
    
    private void stopTimer() {
        mHandler.removeCallbacks(mUpdater);
        clearSongInfo();
    }
    
    private void toggleUpvote() {
        if (mSongInfo.reddit_id == null) {
            toast(R.string.not_submitted);
        } else if (!loggedIn()) {
            toast(R.string.not_logged_in);
        } else {
            AllSongInfo song = RedditApi.toggleUpvote(getModhash(), getCookie(), mSongInfo);
            onSongInfoChanged(song);
        }
    }
    
    private void toggleDownvote() {
        if (mSongInfo.reddit_id == null) {
            toast(R.string.not_submitted);
        } else if (!loggedIn()) {
            toast(R.string.not_logged_in);
        } else {
            AllSongInfo song = RedditApi.toggleDownvote(getModhash(), getCookie(), mSongInfo);
            onSongInfoChanged(song);
        }
    }
    
    private void toggleSave() {
        if (mSongInfo.reddit_id == null) {
            toast(R.string.not_submitted);
        } else if (!loggedIn()) {
            toast(R.string.not_logged_in);
        } else {
            AllSongInfo song = RedditApi.toggleSave(getModhash(), getCookie(), mSongInfo);
            onSongInfoChanged(song);
        }
    }
    
    public String getUser() {
        return mPreferences.getString(PREF_USER, null);
    }
    
    public String getModhash() {
        return mPreferences.getString(PREF_MODHASH, null);
    }
    
    public void setModhash(String modhash) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_MODHASH, modhash);
        editor.commit();
    }
    
    public String getCookie() {
        return mPreferences.getString(PREF_COOKIE, null);
    }
    
    public void registerPlaystateListener(PlaystateChangedListener listener) {
        mListeners.add(listener);
        listener.onPlaystateChanged(isPlaying());
    }
    
    public void unregisterPlaystateListener(PlaystateChangedListener listener) {
        mListeners.remove(listener);
    }
    
    public boolean loggedIn() {
        return getUser() != null && getModhash() != null && getCookie() != null;
    }
    
    // Have remote object save validated login credentials
    public void login(String username, String modhash, String cookie) {
        if (DEBUG) {
            Log.i(TAG, String.format("Setting login info: username=%s modhash=%s cookie=%s", username, modhash, cookie));
        }
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_USER, username);
        editor.putString(PREF_MODHASH, modhash);
        editor.putString(PREF_COOKIE, cookie);
        editor.commit();
        toast(getString(R.string.now_logged_in_as) + " " + username);
    }
    
    public void logout() {
        if (loggedIn()) {
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.remove(PREF_USER);
            editor.remove(PREF_MODHASH);
            editor.remove(PREF_COOKIE);
            editor.commit();
            
            if (mSongInfo != null) {
                mSongInfo.upvoted = false;
                mSongInfo.downvoted = false;
                mSongInfo.saved = false;
                onSongInfoChanged(mSongInfo);
            }
            
            toast(R.string.now_logged_out);
        } else {
            toast(R.string.already_logged_out);
        }
    }
    
    private void updateWidget() {
        Intent intent = new Intent(ACTION_UPDATE_WIDGET);
        Bundle bundle = new Bundle();
        bundle.putParcelable(MusicService.KEY_SONG_INFO, mSongInfo);
        bundle.putString(MusicService.KEY_STREAM_NAME, mStream.name);
        bundle.putBoolean(KEY_IS_PLAYING, isPlaying());
        intent.putExtras(bundle);
        mContext.sendBroadcast(intent);
    }
    
    private void toast(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }
    
    private void toast(String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
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
        mNotification = new Notification(R.drawable.ic_stat_notify_playing, text, System.currentTimeMillis());
        updateNotification();
        startForeground(NOTIFICATION, mNotification);
    }
    
    /**
     * Not only removes the notification (as directed by the 'true' argument)
     * but also sets the service to run in the background again
     */
    private void hideNotification() {
        stopForeground(true);
        mNotification = null;
    }
    
    /**
     * Updates the notification when there is a song or stream change. 
     */
    private void updateNotification() {
        // Drop in some dummy text for artist and title until the service grabs the appropriate info.
        updateNotification(getString(R.string.loading_notification), "", mStream.name);
    }
    private void updateNotification(String artist, String title, String station) {
        if (mNotification != null) {
            NotificationManager nm = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
            
            // Try and reuse the contentIntent.
            PendingIntent contentIntent;
            if (mNotification.contentIntent == null) {
                contentIntent = PendingIntent.getActivity(mContext, 0,
                        new Intent(mContext, MainActivity.class), 0);
            } else {
                contentIntent = mNotification.contentIntent;
            }
            
            // Update the title and text of the notification then send it off to be updated.
            mNotification.setLatestEventInfo(mContext,
                    "Playing " + station + " Stream",
                    artist + (title.length() > 0 ? " - " + title : ""),
                    contentIntent);
            nm.notify(NOTIFICATION, mNotification);
        }
    }
    
    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }
    
    private void clearSongInfo() {
        mSongInfo = new AllSongInfo();
        final String filler = getString(R.string.info_filler);
        mSongInfo.artist = filler;
        mSongInfo.title = filler;
        mSongInfo.genre = filler;
        mSongInfo.redditor = filler;
        mSongInfo.playlist = filler;
        mSongInfo.votes = 0;
        mSongInfo.downvoted = false;
        mSongInfo.upvoted = false;
        mSongInfo.saved = false;
        
        updateWidget();
        
        // Send the entire songinfo object
        Intent intent = new Intent(ACTION_SONG_INFO_CHANGED);
        intent.putExtra(KEY_SONG_INFO, mSongInfo);
        mContext.sendBroadcast(intent);
    }
    
    // Used by the media player for acting when it is prepared
    // Should just start the music playback
    private final MediaPlayer.OnPreparedListener mPreparedListener = new MediaPlayer.OnPreparedListener() {
        @Override
        public void onPrepared(MediaPlayer mp) {
            mp.start();
            startTimer();
            Iterator<PlaystateChangedListener> listeners = mListeners.iterator();
            while (listeners.hasNext()) {
                listeners.next().onPlaystateChanged(true);
            }
        }
    };
}
