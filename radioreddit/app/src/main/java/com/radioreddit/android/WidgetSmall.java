package com.radioreddit.android;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.util.Log;
import android.widget.RemoteViews;

public class WidgetSmall extends WidgetBase {
    private static final String TAG = "SmallWidget";

    private static final ComponentName SELF = new ComponentName(
            "com.radioreddit.android", "com.radioreddit.android.WidgetSmall");

    @Override
    protected RemoteViews generateViews(Context context) {
        return new RemoteViews(context.getPackageName(), R.layout.widget_small);
    }

    @Override
    protected void updateStreamName(Context context, String station, RemoteViews views) {
        views.setTextViewText(R.id.widget_stream, station);
    }

    @Override
    protected void updateSongInfo(Context context, AllSongInfo song, RemoteViews views) {
        if (DEBUG) {
            Log.d(TAG, "++Update Song Info++");
        }

        if (song == null) {
            return;
        }

        // Display the artist and track info.
        views.setTextViewText(R.id.widget_artist, song.artist);
        views.setTextViewText(R.id.widget_title, song.title);
        final Resources resources = context.getResources();
        views.setTextViewText(R.id.widget_votes, Integer.toString(song.votes));
        if (song.upvoted) {
            views.setImageViewResource(R.id.widget_upvote, R.drawable.up_on);
            views.setImageViewResource(R.id.widget_downvote, R.drawable.down_off);
            views.setTextColor(R.id.widget_votes, resources.getColor(R.color.upvote_orange));
        } else if (song.downvoted) {
            views.setImageViewResource(R.id.widget_upvote, R.drawable.up_off);
            views.setImageViewResource(R.id.widget_downvote, R.drawable.down_on);
            views.setTextColor(R.id.widget_votes, resources.getColor(R.color.downvote_blue));
        } else {
            views.setImageViewResource(R.id.widget_upvote, R.drawable.up_off);
            views.setImageViewResource(R.id.widget_downvote, R.drawable.down_off);
            views.setTextColor(R.id.widget_votes, resources.getColor(R.color.novote_grey));
        }
    }

    @Override
    protected void updatePlaystate(Context context, boolean isPlaying, RemoteViews views) {
        if (isPlaying) {
            views.setImageViewResource(R.id.widget_toggle_play, R.drawable.stop);
        } else {
            views.setImageViewResource(R.id.widget_toggle_play, R.drawable.play);
        }
    }

    @Override
    protected void bindButtons(Context context, RemoteViews views) {
        // Link to main activity.
        final Intent mainIntent = new Intent(context, MainActivity.class);
        final PendingIntent mainPending = PendingIntent.getActivity(context, 0, mainIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_icon, mainPending);

        // Author's Note: pendingintents have some weird behavior, they are
        //  recycled if the action and data uri are the same, but not the extras.
        //  Using unique request codes for each pendingintent is hack to get them
        //  to behave as expected since that value currently goes unused. A "cleaner"
        //  solution ought to be worked up down the road.

        // Send upvote command to service.
        final Intent upvoteIntent = new Intent(MusicService.ACTION_PLAYER_COMMAND);
        upvoteIntent.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_UPVOTE);
        final PendingIntent upvotePending = PendingIntent.getBroadcast(context, 0, upvoteIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_upvote, upvotePending);

        // Send downvote command to service.
        final Intent downvoteIntent = new Intent(MusicService.ACTION_PLAYER_COMMAND);
        downvoteIntent.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_DOWNVOTE);
        final PendingIntent downvotePending =
                PendingIntent.getBroadcast(context, 1, downvoteIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_downvote, downvotePending);

        // Spools up the service if it isn't running in the
        //  background and sends a toggle playstate command.
        final Intent toggleIntent = new Intent(MusicService.ACTION_PLAYER_COMMAND);
        toggleIntent.setComponent(new ComponentName(
                "com.radioreddit.android", "com.radioreddit.android.MusicService"));
        toggleIntent.putExtra(MusicService.KEY_COMMAND, MusicService.CMD_TOGGLE_PLAY);
        PendingIntent togglePending = PendingIntent.getService(context, 2, toggleIntent, 0);
        views.setOnClickPendingIntent(R.id.widget_toggle_play, togglePending);
    }

    @Override
    protected ComponentName getComponentName() {
        return SELF;
    }
}
