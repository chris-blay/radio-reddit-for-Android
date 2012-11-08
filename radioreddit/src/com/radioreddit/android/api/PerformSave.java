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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import android.net.ParseException;
import android.os.AsyncTask;
import android.util.Log;

public class PerformSave extends AsyncTask<String, Integer, Void> {
    @Override
    protected Void doInBackground(String... params) {
        final String modhash = params[0];
        final String cookie = params[1];
        final String id = params[2];
        final String type = params[3];
        
        // Prepare POST with cookie and execute it
        try {
            final HttpClient httpClient = new DefaultHttpClient();
            final HttpPost httpPost = new HttpPost("http://www.reddit.com/api/" + type);
            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("id", id));
            nameValuePairs.add(new BasicNameValuePair("uh", modhash));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            // Using HttpContext, CookieStore, and friends didn't work
            httpPost.setHeader("Cookie", "reddit_session=" + cookie);
            httpPost.setHeader("User-Agent", RedditApi.USER_AGENT);
            httpClient.execute(httpPost);
            // We just assume that everything worked so there's no need to check the response
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
