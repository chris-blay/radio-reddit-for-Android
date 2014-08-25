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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.radioreddit.android.actionbarcompat.ActionBarActivity;

public class MainActivity extends ActionBarActivity implements PlaystateChangedListener {
    private static final String TAG = "MainActivty";
    private static final boolean DEBUG = false;

    // Used for displaying dialogs
    private static final int DIALOG_TUNE = 1;
    private static final int DIALOG_INFO = 2;

    private Context mContext;
    private Resources mResources;

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

    // Connection stuff for the music playing service
    private MusicService mService;
    private final ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = ((MusicService.MusicBinder) service).getService();
            mService.registerPlaystateListener(MainActivity.this);
        }
        @Override
        public void onServiceDisconnected(ComponentName className) {
            mService.unregisterPlaystateListener(MainActivity.this);
            mService = null;
        }
    };

    private BroadcastReceiver mBackendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
            case MusicService.ACTION_SONG_INFO_CHANGED:
                if (DEBUG) {
                    Log.d(TAG, "++Song Info Intent Received++");
                }
                displaySongInfo((AllSongInfo) intent.getParcelableExtra(MusicService.KEY_SONG_INFO));
                break;
            case MusicService.ACTION_STATION_CHANGED:
                if (DEBUG) {
                    Log.d(TAG, "++Station Change Intent Received++");
                }
                mSongStream.setText(intent.getExtras().getString(MusicService.KEY_STREAM_NAME));
                break;
            case MusicService.ACTION_REQUEST_UPDATE:
                if (this.getResultCode() == Activity.RESULT_OK) {
                    if (DEBUG) {
                        Log.d(TAG, "++Update Intent Received Ok++");
                    }
                    final Bundle bundle = this.getResultExtras(false);
                    if (bundle != null) {
                        displaySongInfo((AllSongInfo) bundle.getParcelable(MusicService.KEY_SONG_INFO));
                        mSongStream.setText(bundle.getString(MusicService.KEY_STREAM_NAME));
                    }
                }
                break;
            }
        }
    };

    // Cancel listener for the tune dialog
    private final DialogInterface.OnClickListener mTuneDialogCancelListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            dialog.dismiss();
        }
    };

    // Cancel listener for the info dialog. Needs to remove the dialog so it doesn't show up again on rotate
    private final DialogInterface.OnClickListener mInfoDialogCancelListener = new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            removeDialog(DIALOG_INFO);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (DEBUG) {
            Log.d(TAG, "++onCreate++");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // Initialize member variables
        mContext = getApplicationContext();
        mResources = getResources();
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
        findViewById(R.id.info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog(DIALOG_INFO);
            }
        });

        // Start the music playing service this allows the service to continue after the application has lost focus
        startService(new Intent(mContext, MusicService.class));
    }

    @Override
    protected void onStart() {
        if (DEBUG) {
            Log.d(TAG, "++onStart++");
        }
        super.onStart();
        bindService(new Intent(mContext, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onResume() {
        if (DEBUG) {
            Log.d(TAG, "++onResume++");
        }
        super.onResume();
        final IntentFilter filter = new IntentFilter();
        filter.addAction(MusicService.ACTION_SONG_INFO_CHANGED);
        filter.addAction(MusicService.ACTION_STATION_CHANGED);
        filter.addAction(MusicService.ACTION_REQUEST_UPDATE);
        registerReceiver(mBackendReceiver, filter);

        // Send a loopback call to the service for the most up to date data.
        final Intent intent = new Intent(MusicService.ACTION_REQUEST_UPDATE);
        mContext.sendOrderedBroadcast(intent, null, mBackendReceiver, null, Activity.RESULT_FIRST_USER, null, null);
    }

    @Override
    public void onPause() {
        if (DEBUG) {
            Log.d(TAG, "++onPause++");
        }
        super.onPause();
        unregisterReceiver(mBackendReceiver);
    }

    @Override
    public void onDestroy() {
        if (DEBUG) {
            Log.d(TAG, "++OnDestroy++");
        }
        super.onDestroy();

        // Disconnect from the music playing service if it is connected
        if (mService != null) {
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem loginItem = menu.findItem(R.id.menu_login);
        MenuItem logoutItem = menu.findItem(R.id.menu_logout);

        // Changes the visibility of login/logout items depending on the login
        //  state. On Version 11+ invalidateOptionMenu() will need to be called
        //  from login() and logout() to update the menuitems in the actionbar
        if (mService != null) {
            if (mService.loggedIn()) {
                loginItem.setVisible(false);
                logoutItem.setVisible(true);
            } else {
                loginItem.setVisible(true);
                logoutItem.setVisible(false);
            }
        }
        return super.onPrepareOptionsMenu(menu);
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
            case R.id.menu_login:
                login();
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
            builder.setNeutralButton(android.R.string.cancel, mTuneDialogCancelListener);
            builder.setItems(MusicService.STREAM_NAMES, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Intent chgStream = new Intent(MusicService.ACTION_PLAYER_COMMAND);
                    chgStream.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_CHANGE_STREAM);
                    chgStream.putExtra("stream_id", which);
                    sendBroadcast(chgStream);
                }
            });
            return builder.create();
        case DIALOG_INFO:
            builder.setTitle(R.string.info_dialog_title);
            builder.setNeutralButton(android.R.string.ok, mInfoDialogCancelListener);
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
        mNoInternet.setVisibility(View.GONE);
        mSongTitle.setText(songInfo.title);
        mSongArtist.setText(songInfo.artist);
        mSongGenre.setText(songInfo.genre);
        mRedditor.setText(songInfo.redditor);
        mPlaylist.setText(songInfo.playlist);
        displayStatus(songInfo);
    }

    private void displayStatus(AllSongInfo songinfo) {
        mVotes.setText(String.valueOf(songinfo.votes));
        if (songinfo.upvoted) {
            mUpvote.setImageResource(R.drawable.up_on);
            mDownvote.setImageResource(R.drawable.down_off);
            mVotes.setTextColor(mResources.getColor(R.color.upvote_orange));
        } else if (songinfo.downvoted) {
            mUpvote.setImageResource(R.drawable.up_off);
            mDownvote.setImageResource(R.drawable.down_on);
            mVotes.setTextColor(mResources.getColor(R.color.downvote_blue));
        } else {
            mUpvote.setImageResource(R.drawable.up_off);
            mDownvote.setImageResource(R.drawable.down_off);
            mVotes.setTextColor(mResources.getColor(R.color.novote_grey));
        }
        if (songinfo.saved) {
            mSave.setImageResource(R.drawable.star_on);
        } else {
            mSave.setImageResource(R.drawable.star_off);
        }
    }

    // Tells the service to toggle the upvote on the currently playing track
    private void toggleUpvote() {
        if (!mService.loggedIn()) {
            login();
        } else {
            final Intent cmdUpvote = new Intent(MusicService.ACTION_PLAYER_COMMAND);
            cmdUpvote.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_UPVOTE);
            sendBroadcast(cmdUpvote);
        }
    }

    // Tells the service to toggle the downvote on the currently playing track
    private void toggleDownvote() {
        if (!mService.loggedIn()) {
            login();
        } else {
            Intent cmdDownvote = new Intent(MusicService.ACTION_PLAYER_COMMAND);
            cmdDownvote.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_DOWNVOTE);
            sendBroadcast(cmdDownvote);
        }
    }

    // Tells the service to toggle the save state of the current track
    private void toggleSave() {
        if (!mService.loggedIn()) {
            login();
        } else {
            Intent cmdSave = new Intent(MusicService.ACTION_PLAYER_COMMAND);
               cmdSave.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_SAVE);
            sendBroadcast(cmdSave);
        }
    }

    private void togglePlay() {
        Intent togglePlayIntent = new Intent(MusicService.ACTION_PLAYER_COMMAND);
        togglePlayIntent.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_TOGGLE_PLAY);
        sendBroadcast(togglePlayIntent);
    }

    // Called by the backend service whenever the stream starts or stops playing
    public void onPlaystateChanged(boolean isPlaying) {
        if (isPlaying) {
            mPlay.setImageResource(R.drawable.stop);
        } else {
            mPlay.setImageResource(R.drawable.play);
        }
    }

    private void login() {
        startActivity(new Intent(mContext, LoginActivity.class));
    }

    private void logout() {
        if (mService != null) {
            mService.logout();
        }
    }

}
