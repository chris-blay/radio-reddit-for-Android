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

import android.net.ParseException;
import android.os.AsyncTask;
import android.util.Log;

import com.radioreddit.android.AllSongInfo;
import com.radioreddit.android.MusicService;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class GetSongInfo extends AsyncTask<String, Integer, Boolean> {
    private AllSongInfo mSong;

    private MusicService mService;
    public GetSongInfo(MusicService service, AllSongInfo song) {
        mService = service;
        mSong = song;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        final String cookie = params[0]; // This will be null if not logged in.

        // Prepare GET with cookie, execute it, parse response as JSON.
        JSONObject response = null;
        try {
            final HttpClient httpClient = new DefaultHttpClient();
            final List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("url", mSong.reddit_url));
            final HttpGet httpGet = new HttpGet("http://www.reddit.com/api/info.json?"
                    + URLEncodedUtils.format(nameValuePairs, "utf-8"));
            if (cookie != null) {
                // Using HttpContext, CookieStore, and friends didn't work.
                httpGet.setHeader("Cookie", "reddit_session=" + cookie);
            }
            httpGet.setHeader("User-Agent", RedditApi.USER_AGENT);
            final HttpResponse httpResponse = httpClient.execute(httpGet);
            response = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
        } catch (UnsupportedEncodingException e) {
            Log.i(RedditApi.TAG, "UnsupportedEncodingException while getting song info", e);
        } catch (ClientProtocolException e) {
            Log.i(RedditApi.TAG, "ClientProtocolException while getting song info", e);
        } catch (IOException e) {
            Log.i(RedditApi.TAG, "IOException while getting song info", e);
        } catch (ParseException e) {
            Log.i(RedditApi.TAG, "ParseException while getting song info", e);
        } catch (JSONException e) {
            Log.i(RedditApi.TAG, "JSONException while getting song info", e);
        }

        // Check for failure.
        if (response == null) {
            Log.i(RedditApi.TAG, "Response is null");
            return false;
        }

        // Get the info we want.
        final JSONObject data1 = response.optJSONObject("data");
        if (data1 == null) {
            Log.i(RedditApi.TAG, "First data is null");
            return false;
        }
        final String modhash = data1.optString("modhash", "");
        if (modhash.length() > 0) {
            mService.setModhash(modhash);
        }
        final JSONArray children = data1.optJSONArray("children");
        if (children == null) {
            Log.i(RedditApi.TAG, "Children is null");
            return false;
        }
        final JSONObject child = children.optJSONObject(0);
        if (child == null) {
            // This is common if the song hasn't been submitted to reddit yet
            //  so we intentionally don't log this case.
            return false;
        }
        final String kind = child.optString("kind");
        if (kind == null) {
            Log.i(RedditApi.TAG, "Kind is null");
            return false;
        }
        final JSONObject data2 = child.optJSONObject("data");
        if (data2 == null) {
            Log.i(RedditApi.TAG, "Second data is null");
            return false;
        }
        final String id = data2.optString("id");
        if (id == null) {
            Log.i(RedditApi.TAG, "Id is null");
            return false;
        }
        final int score = data2.optInt("score");
        Boolean likes = null;
        if (!data2.isNull("likes")) {
            likes = data2.optBoolean("likes");
        }
        final boolean saved = data2.optBoolean("saved");

        // Modify song with collected info.
        mSong.reddit_id = kind + "_" + id;
        mSong.upvoted = (likes != null && likes);
        mSong.downvoted = (likes != null && !likes);
        mSong.votes = score;
        mSong.saved = saved;

        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (!success) {
            // Set default values for stuff we couldn't get.
            mSong.reddit_id = null;
            mSong.upvoted = false;
            mSong.downvoted = false;
            mSong.votes = 0;
            mSong.saved = false;
        }
        mService.onSongInfoChanged(mSong);
    }
}
