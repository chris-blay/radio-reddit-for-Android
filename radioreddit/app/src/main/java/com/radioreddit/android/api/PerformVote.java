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

import com.radioreddit.android.MusicService;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PerformVote extends AsyncTask<String, Integer, Void> {
    @Override
    protected Void doInBackground(String... params) {
        final String modhash = params[0];
        final String cookie = params[1];
        final String id = params[2];
        final String dir = params[3];

        if (MusicService.DEBUG) {
            Log.i(RedditApi.TAG, String.format("PerformVote args: modhash=%s cookie=%s id=%s dir=%s", modhash, cookie, id, dir));
        }

        // Prepare POST with cookie and execute it
        try {
            final HttpClient httpClient = new DefaultHttpClient();
            final HttpPost httpPost = new HttpPost("http://www.reddit.com/api/vote");
            final List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("id", id));
            nameValuePairs.add(new BasicNameValuePair("dir", dir));
            nameValuePairs.add(new BasicNameValuePair("uh", modhash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Using HttpContext, CookieStore, and friends didn't work
            httpPost.setHeader("Cookie", String.format("reddit_session=\"%s\"", cookie));
            httpPost.setHeader("User-Agent", RedditApi.USER_AGENT);
            if (MusicService.DEBUG) {
                // Do some extra work when debugging to print the response
                final ResponseHandler<String> responseHandler = new BasicResponseHandler();
                final String response = httpClient.execute(httpPost, responseHandler);
                Log.i(RedditApi.TAG, "Reddit vote response: " + response);
            } else {
                // Otherwise just assume everything works out for now
                // TODO: Check for error responses and inform user of the problem
                httpClient.execute(httpPost);
            }
        } catch (UnsupportedEncodingException e) {
            Log.i(RedditApi.TAG, "UnsupportedEncodingException while performing vote", e);
        } catch (ClientProtocolException e) {
            Log.i(RedditApi.TAG, "ClientProtocolException while performing vote", e);
        } catch (IOException e) {
            Log.i(RedditApi.TAG, "IOException while performing vote", e);
        } catch (ParseException e) {
            Log.i(RedditApi.TAG, "ParseException while performing vote", e);
        }

        return null;
    }

    @Override
    protected void onPostExecute(Void unused) {
        // This method intentionally does nothing
    }
}
