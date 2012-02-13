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
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.RemoteViews;

public abstract class BaseWidget extends AppWidgetProvider {
    private static final String TAG = "BaseWidget";
    private static final boolean DEBUG = false;
    
    protected abstract RemoteViews generateViews(Context context);
    protected abstract void updateSongInfo(Context context, AllSongInfo song, RemoteViews views);
    protected abstract void updateStreamName(Context context, String station, RemoteViews views);
    protected abstract void updatePlaystate(Context context, boolean isPlaying, RemoteViews views);
    protected abstract void bindButtons(Context context, RemoteViews views);
    protected abstract ComponentName getComponentName();
    
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Send a loopback broadcast to the service asking for the most current data
        final Intent updateIntent = new Intent(MusicService.ACTION_REQUEST_UPDATE);
        final Bundle bundle = this.getResultExtras(true);
        bundle.putIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds);
        context.sendOrderedBroadcast(updateIntent, null, this, null, Activity.RESULT_FIRST_USER, null, bundle);
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (DEBUG) {
            Log.d(TAG, "++Intent Received++");
        }
        
        final String action = intent.getAction();
        if (action.equals(MusicService.ACTION_REQUEST_UPDATE)) {
            // Request update is the result of a sendOrderedBroadcast loopback
            //  from within the initial onUpdate call, usually called when a
            //  widget is first added and it needs to get the most current data
            if (this.getResultCode() == Activity.RESULT_OK) {
                final Bundle bundle = this.getResultExtras(false);
                if (bundle != null) {
                    update(context, bundle);
                }
            }
        } else if (action.equals(MusicService.ACTION_UPDATE_WIDGET)) {
            // Update widget is the result of the service's internal update widget.
            // Called whenever new content or state info needs to be passed to the widget.
            // Authors Note: Versions of Android pre-11 have a new instance of remoteviews
            //  being used to update the widget views each time there's a change. Because of this,
            //  this section is overloaded. If using a minsdk of 11+ the partiallyUpdateWidget method
            //  ought to be used with intents for each item; songinfo, streamName, and
            //  playstate (though in truth the playstatelistener ought to be used instead, but
            //  that's a whole other ball game)
            final Bundle bundle = intent.getExtras();
            if (bundle != null) {
                update(context, bundle);
            }
        }
        
        super.onReceive(context, intent);
    }
    
    private void update(Context context, Bundle bundle) {
        final int[] appWidgetIds = bundle.getIntArray(AppWidgetManager.EXTRA_APPWIDGET_IDS);
        final AllSongInfo songinfo = bundle.getParcelable(MusicService.KEY_SONG_INFO);
        final String streamName = bundle.getString(MusicService.KEY_STREAM_NAME);
        final boolean isPlaying = bundle.getBoolean(MusicService.KEY_IS_PLAYING);
        
        // Get the remoteviews associated with widget's specific layout
        RemoteViews views = generateViews(context);
        // Push the updated song info to the remoteviews then get
        //  the views back for further updating
        updateSongInfo(context, songinfo, views);
        // Push the updated stream name
        updateStreamName(context, streamName, views);
        // Push the updated playstate
        updatePlaystate(context, isPlaying, views);
        // Push the updated playstate
        bindButtons(context, views);
        // Update the widgets themselves, either using the ids for a small selection
        //  or the componentname for every instance of said widget
        final AppWidgetManager awm = AppWidgetManager.getInstance(context);
        if (appWidgetIds != null) {
            awm.updateAppWidget(appWidgetIds, views);
        } else {
            awm.updateAppWidget(getComponentName(), views);
        }
    }
}

