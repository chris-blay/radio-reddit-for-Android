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

package com.radioreddit.android.api;

import com.radioreddit.android.AllSongInfo;
import com.radioreddit.android.MusicService;

public class RedditApi {
    public static final String TAG = "RedditApi";
    public static final String USER_AGENT = "radio reddit for android 6";
    
    private static final String VOTE_UP = "1";
    private static final String VOTE_RECIND = "0";
    private static final String VOTE_DOWN = "-1";
    
    private static final String SAVE = "save";
    private static final String UNSAVE = "unsave";
    
    public static void requestLogin(LoginResultCallback callback, String username, String password) {
        new PerformLogin(callback).execute(username, password);
    }
    
    public static void requestSongInfo(MusicService service, String cookie, String url) {
        new GetStationStatus(service, cookie).execute(url);
    }
    
    public static AllSongInfo toggleUpvote(String modhash, String cookie, AllSongInfo info) {
        if (info.downvoted) {
            info.downvoted = false;
        }
        if (info.upvoted) {
            info.upvoted = false;
            info.votes--;
            new PerformVote().execute(modhash, cookie, info.reddit_id, VOTE_RECIND);
        } else {
            info.upvoted = true;
            info.votes++;
            new PerformVote().execute(modhash, cookie, info.reddit_id, VOTE_UP);
        }
        return info;
    }
    
    public static AllSongInfo toggleDownvote(String modhash, String cookie, AllSongInfo info) {
        if (info.upvoted) {
            info.upvoted = false;
        }
        if (info.downvoted) {
            info.downvoted = false;
            info.votes++;
            new PerformVote().execute(modhash, cookie, info.reddit_id, VOTE_RECIND);
        } else {
            info.downvoted = true;
            info.votes--;
            new PerformVote().execute(modhash, cookie, info.reddit_id, VOTE_DOWN);
        }
        return info;
    }
    
    public static AllSongInfo toggleSave(String modhash, String cookie, AllSongInfo info) {
        if (info.saved) {
            info.saved = false;
            new PerformSave().execute(modhash, cookie, info.reddit_id, UNSAVE);
        } else {
            info.saved = true;
            new PerformSave().execute(modhash, cookie, info.reddit_id, SAVE);
        }
        return info;
    }
}

