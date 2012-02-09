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

import java.util.Timer;
import java.util.TimerTask;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.radioreddit.android.actionbarcompat.ActionBarActivity;
import com.radioreddit.android.api.RedditApi;
import com.radioreddit.android.api.Relay;
import com.radioreddit.android.api.Stream;

public class MainActivity extends ActionBarActivity {
    // Used for preference names
    public static final String PREF_STREAM = "stream";
    public static final String PREF_USER = "user";
    public static final String PREF_MODHASH = "modhash";
    public static final String PREF_COOKIE = "cookie";
    
    // Used for displaying dialogs
    private static final int DIALOG_TUNE = 1;
    private static final int DIALOG_INFO = 2;
    
    // How often we should request updated song information from the API
    private static final int UPDATE_INTERVAL = 15000; // 15s
    
    private static final Stream[] STREAMS = {
        new Stream("main", "/api/", new Relay[] {new Relay("main.radioreddit.com:8080")}),
        new Stream("electronic", "/api/electronic/", new Relay[] {new Relay("electronic.radioreddit.com:8080")}),
        new Stream("rock", "/api/rock/", new Relay[] {new Relay("rock.radioreddit.com:8080")}),
        new Stream("metal", "/api/metal/", new Relay[] {new Relay("texas.radioreddit.com:8090")}),
        new Stream("indie", "/api/indie/", new Relay[] {new Relay("indie.radioreddit.com:8080")}),
        new Stream("hiphop", "/api/hiphop/", new Relay[] {new Relay("hiphop.radioreddit.com:8080")}),
        new Stream("random", "/api/random/", new Relay[] {new Relay("random.radioreddit.com:8080")}),
        new Stream("talk", "/api/talk/", new Relay[] {new Relay("talk.radioreddit.com:8080")}),
    };
    private static final CharSequence[] STREAM_NAMES = {
        "main",
        "electronic",
        "rock",
        "metal",
        "indie",
        "hiphop",
        "random",
        "talk",
    };
    
    private Context mContext;
    private Resources mResources;
    private SharedPreferences mPreferences;
    private Timer mTimer;
    private Stream mStream;
    
    private TextView mNoInternet;
    private ImageButton mUpvote;
    private TextView mVotes;
    private ImageButton mDownvote;
    private TextView mSongStream;
    private TextView mSongTitle;
    private TextView mSongArtist;
    private TextView mSongGenre;
    private TextView mRedditor;
    private TextView mPlaylist;
    private ImageButton mSave;
    private ImageButton mPlay;
    private ImageButton mInfo;
    private AllSongInfo mSongInfo;
    
    // Connection stuff for the music playing service 
    private MusicService mService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((MusicService.MusicBinder) service).getService();
            mService.attach(MainActivity.this);
            if (mService.isPlaying()) {
                mPlay.setImageResource(R.drawable.stop);
            } else {
                mPlay.setImageResource(R.drawable.play);
            }
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService = null;
        }
    };
    
    // Used for the cancel listener in both dialogs
    private final DialogInterface.OnClickListener mDialogCancelListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // Initialize member variables
        mContext = getApplicationContext();
        mResources = getResources();
        mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mNoInternet = (TextView) findViewById(R.id.no_internet);
        mUpvote = (ImageButton) findViewById(R.id.upvote);
        mVotes = (TextView) findViewById(R.id.votes);
        mDownvote = (ImageButton) findViewById(R.id.downvote);
        mSongStream = (TextView) findViewById(R.id.song_stream);
        mSongTitle = (TextView) findViewById(R.id.song_title);
        mSongArtist = (TextView) findViewById(R.id.song_artist);
        mSongGenre = (TextView) findViewById(R.id.song_genre);
        mRedditor = (TextView) findViewById(R.id.redditor);
        mPlaylist = (TextView) findViewById(R.id.playlist);
        mSave = (ImageButton) findViewById(R.id.save);
        mPlay = (ImageButton) findViewById(R.id.play);
        mInfo = (ImageButton) findViewById(R.id.info);
        
        // Setup actions for button clicks
        mUpvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleUpvote();
            }
        });
        mDownvote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleDownvote();
            }
        });
        mSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleSave();
            }
        });
        mPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePlay();
            }
        });
        mInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_INFO);
            }
        });
        
        // Set saved stream from preferences or use default
        mStream = STREAMS[mPreferences.getInt(PREF_STREAM, 0)];
        mSongStream.setText(mStream.name);
        
        // We don't know what song is playing yet
        displaySongInfo(null);
        
        // Start the music playing service this allows the service to continue after the application has lost focus
        startService(new Intent(mContext, MusicService.class));
        
        // Connect to the music playing service
        bindService(new Intent(mContext, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
        
        // Inform user of currently logged in user
        final String user = getUser();
        if (user == null) {
            toast(R.string.not_logged_in);
        } else {
            toast(getString(R.string.currently_logged_in_as) + " " + getUser());
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        startTimer();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        stopTimer();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        
        // Disconnect from the music playing service if it is connected
        if (mService != null) {
            mService.detatch();
            unbindService(mConnection);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main, menu);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Intentionally do nothing
                break;
            case R.id.menu_tune:
                showDialog(DIALOG_TUNE);
                break;
            case R.id.menu_logout:
                logout();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        switch (id) {
        case DIALOG_TUNE:
            builder.setTitle(R.string.tune_dialog_title);
            builder.setNeutralButton(android.R.string.cancel, mDialogCancelListener);
            builder.setItems(STREAM_NAMES, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    stopTimer();
                    mStream = STREAMS[which];
                    mSongStream.setText(mStream.name);
                    startTimer();
                    if (mService != null && mService.isPlaying()) {
                        mService.play("http://" + mStream.relays[0].server);
                    }
                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putInt(PREF_STREAM, which);
                    editor.commit();
                    toast(getString(R.string.now_tuned_to) + " " + mStream.name);
                }
            });
            return builder.create();
        case DIALOG_INFO:
            builder.setTitle(R.string.info_dialog_title);
            builder.setNeutralButton(android.R.string.ok, mDialogCancelListener);
            builder.setMessage(Html.fromHtml(getString(R.string.info_text)));
            Dialog dialog = builder.create();
            dialog.show();
            ((TextView) dialog.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());
            return dialog;
        }
        return null;
    }
    
    // Called only after the song info changes
    public void displaySongInfo(AllSongInfo songInfo) {
        mSongInfo = songInfo;
        if (songInfo == null) {
            final String filler = getString(R.string.info_filler);
            if (mService != null) {
                mService.stop();
                mPlay.setImageResource(R.drawable.play);
            }
            mNoInternet.setVisibility(View.VISIBLE);
            mSongTitle.setText(filler);
            mSongArtist.setText(filler);
            mSongGenre.setText(filler);
            mRedditor.setText(filler);
            mPlaylist.setText(filler);
        } else {
            mNoInternet.setVisibility(View.GONE);
            mSongTitle.setText(songInfo.title);
            mSongArtist.setText(songInfo.artist);
            mSongGenre.setText(songInfo.genre);
            mRedditor.setText(songInfo.redditor);
            mPlaylist.setText(songInfo.playlist);
        }
        displayStatus();
    }
    
    // Called whenever the song status or info changes
    private void displayStatus() {
        if (mSongInfo == null) {
            final String filler = getString(R.string.info_filler);
            mVotes.setText(filler);
            mUpvote.setImageResource(R.drawable.up_off);
            mDownvote.setImageResource(R.drawable.down_off);
            mVotes.setTextColor(mResources.getColor(R.color.novote_grey));
            mSave.setImageResource(R.drawable.star_off);
        } else {
            mVotes.setText(String.valueOf(mSongInfo.votes));
            if (mSongInfo.upvoted) {
                mUpvote.setImageResource(R.drawable.up_on);
                mDownvote.setImageResource(R.drawable.down_off);
                mVotes.setTextColor(mResources.getColor(R.color.upvote_orange));
            } else if (mSongInfo.downvoted) {
                mUpvote.setImageResource(R.drawable.up_off);
                mDownvote.setImageResource(R.drawable.down_on);
                mVotes.setTextColor(mResources.getColor(R.color.downvote_blue));
            } else {
                mUpvote.setImageResource(R.drawable.up_off);
                mDownvote.setImageResource(R.drawable.down_off);
                mVotes.setTextColor(mResources.getColor(R.color.novote_grey));
            }
            if (mSongInfo.saved) {
                mSave.setImageResource(R.drawable.star_on);
            } else {
                mSave.setImageResource(R.drawable.star_off);
            }
        }
    }
    
    private void startTimer() {
        RedditApi.requestSongInfo(MainActivity.this, getCookie(), "http://www.radioreddit.com" + mStream.status + "status.json");
        mTimer = new Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                RedditApi.requestSongInfo(MainActivity.this, getCookie(), "http://www.radioreddit.com" + mStream.status + "status.json");
            }
        }, UPDATE_INTERVAL, UPDATE_INTERVAL);
    }
    
    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
        }
        mTimer = null;
    }
    
    private void toggleUpvote() {
        if (mSongInfo == null) {
            toast(R.string.no_internet);
        } else if (mSongInfo.reddit_id == null) {
            toast(R.string.not_submitted);
        } else if (!loggedIn()) {
            login();
        } else {
            mSongInfo = RedditApi.toggleUpvote(getModhash(), getCookie(), mSongInfo);
            displayStatus();
        }
    }
    
    private void toggleDownvote() {
        if (mSongInfo == null) {
            toast(R.string.no_internet);
        } else if (mSongInfo.reddit_id == null) {
            toast(R.string.not_submitted);
        } else if (!loggedIn()) {
            login();
        } else {
            mSongInfo = RedditApi.toggleDownvote(getModhash(), getCookie(), mSongInfo);
            displayStatus();
        }
    }
    
    private void toggleSave() {
        if (mSongInfo == null) {
            toast(R.string.no_internet);
        } else if (mSongInfo.reddit_id == null) {
            toast(R.string.not_submitted);
        } else if (!loggedIn()) {
            login();
        } else {
            mSongInfo = RedditApi.toggleSave(getModhash(), getCookie(), mSongInfo);
            displayStatus();
        }
    }
    
    private void togglePlay() {
        if (mService == null) {
            return;
        } else if (mSongInfo == null) {
            toast(R.string.no_internet);
            return;
        }
        if (mService.isPlaying()) {
            mService.stop();
            mPlay.setImageResource(R.drawable.play);
        } else {
            mService.play("http://" + mStream.relays[0].server);
            mPlay.setImageResource(R.drawable.stop);
        }
    }
    
    private void login() {
        startActivity(new Intent(mContext, LoginActivity.class));
    }
    
    private String getUser() {
        return mPreferences.getString(PREF_USER, null);
    }
    
    private String getModhash() {
        return mPreferences.getString(PREF_MODHASH, null);
    }
    
    public void setModhash(String modhash) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putString(PREF_MODHASH, modhash);
        editor.commit();
    }
    
    private String getCookie() {
        return mPreferences.getString(PREF_COOKIE, null);
    }
    
    private boolean loggedIn() {
        return getUser() != null && getModhash() != null && getCookie() != null;
    }
    
    private void logout() {
        if (loggedIn()) {
            // Remove modhash and cookie from preferences
            final SharedPreferences.Editor editor = mPreferences.edit();
            editor.remove(PREF_USER);
            editor.remove(PREF_MODHASH);
            editor.remove(PREF_COOKIE);
            editor.commit();
            // Clear up/down vote and star
            if (mSongInfo != null) {
                mSongInfo.upvoted = false;
                mSongInfo.downvoted = false;
                mSongInfo.saved = false;
                displayStatus();
            }
            // Inform user of change
            toast(R.string.now_logged_out);
        } else {
            toast(R.string.already_logged_out);
        }
    }
    
    public void toast(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }
    
    public void toast(String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }
}
