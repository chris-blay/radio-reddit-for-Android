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

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

public class SmallWidget extends BaseWidget {
    private static final String TAG = "SmallWidget";
    private static final boolean DEBUG = false;
    
    private static final ComponentName SELF = new ComponentName(
            "com.radioreddit.android", "com.radioreddit.android.SmallWidget");
    
    @Override
    protected RemoteViews generateViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_small);
    }
    
    @Override
    protected void updateStreamName(Context context, String station, RemoteViews views) {
        // Pass
    }
    
    @Override
    protected void updateSongInfo(Context context, AllSongInfo song, RemoteViews views) {
        if (DEBUG) {
            Log.d(TAG, "++Update Song Info++");
        }
        
        views.setTextViewText(R.id.widget_small_artist, song.artist);
        views.setTextViewText(R.id.widget_small_title, song.title);
    }
    
    @Override
    protected void updatePlaystate(Context context, boolean isPlaying, RemoteViews views) {
        if (isPlaying) {
            views.setImageViewResource(R.id.widget_small_toggle_play, R.drawable.stop);
        } else {
            views.setImageViewResource(R.id.widget_small_toggle_play, R.drawable.play);
        }
    }
    
    @Override
    protected void bindButtons(Context context, RemoteViews views) {
        // Link to main activity
        final Intent intent = new Intent(context, MainActivity.class);
        final PendingIntent pending = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(R.id.widget_small_alien, pending);
        
        // Spools up the service if it isn't running in the background and sends
        //  a toggle playstate command. The requestcode for this toggle play
        //  intent must match the request code for the toggle play intent in the
        //  large widget
        final Intent toggleIntent = new Intent(MusicService.ACTION_PLAYER_COMMAND);
        toggleIntent.setComponent(new ComponentName("com.radioreddit.android", "com.radioreddit.android.MusicService"));
        toggleIntent.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_TOGGLE_PLAY);
        final PendingIntent togglePending = PendingIntent.getService(context, 2, toggleIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_small_toggle_play, togglePending);
    }
    
    @Override
    protected ComponentName getComponentName() {
        return SELF;
    }
}

