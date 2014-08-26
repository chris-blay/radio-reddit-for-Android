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

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
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

public class PerformLogin extends AsyncTask<String, Integer, Boolean> {
    private String mUser;
    private String mModhash;
    private String mCookie;
    private LoginResultCallback mCallback;

    public PerformLogin(LoginResultCallback callback) {
        mCallback = callback;
    }

    @Override
    protected Boolean doInBackground(String... params) {
        final String username = params[0];
        final String password = params[1];

        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return false;
        }

        // Prepare POST, execute it, parse response as JSON.
        JSONObject response;
        try {
            final HttpClient httpClient = new DefaultHttpClient();
            final HttpPost httpPost = new HttpPost("https://ssl.reddit.com/api/login");
            final List<NameValuePair> nameValuePairs = new ArrayList<>();
            nameValuePairs.add(new BasicNameValuePair("user", username));
            nameValuePairs.add(new BasicNameValuePair("passwd", password));
            nameValuePairs.add(new BasicNameValuePair("api_type", "json"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            httpPost.setHeader("User-Agent", RedditApi.USER_AGENT);
            final HttpResponse httpResponse = httpClient.execute(httpPost);
            final String responseBody = EntityUtils.toString(httpResponse.getEntity());
            if (MusicService.DEBUG) {
                Log.i(RedditApi.TAG, "Reddit API login response: " + responseBody);
            }
            response = new JSONObject(responseBody);
            response = response.getJSONObject("json");
        } catch (UnsupportedEncodingException e) {
            Log.i(RedditApi.TAG, "UnsupportedEncodingException while performing login", e);
            return false;
        } catch (ClientProtocolException e) {
            Log.i(RedditApi.TAG, "ClientProtocolException while performing login", e);
            return false;
        } catch (IOException e) {
            Log.i(RedditApi.TAG, "IOException while performing login", e);
            return false;
        } catch (ParseException e) {
            Log.i(RedditApi.TAG, "ParseException while performing login", e);
            return false;
        } catch (JSONException e) {
            Log.i(RedditApi.TAG, "JSONException while performing login", e);
            return false;
        }

        // Check for failure.
        if (response == null) {
            Log.i(RedditApi.TAG, "Response was null while performing login");
            return false;
        }

        // Check for errors.
        final JSONArray errors = response.optJSONArray("errors");
        if (errors == null || errors.length() > 0) {
            Log.i(RedditApi.TAG, "Response has errors while performing login");
            return false;
        }

        // Check for data.
        final JSONObject data = response.optJSONObject("data");
        if (data == null) {
            Log.i(RedditApi.TAG, "Response missing data while performing login");
            return false;
        }

        // Get modhash and cookie from data.
        mUser = username;
        mModhash = data.optString("modhash");
        mCookie = data.optString("cookie");
        if (mModhash.length() == 0 || mCookie.length() == 0) {
            Log.i(RedditApi.TAG, "Response missing modhash/cookie while performing login");
            return false;
        }

        return true;
    }

    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            mCallback.onLoginResult(true, mUser, mModhash, mCookie);
        } else {
            mCallback.onLoginResult(false, null, null, null);
        }
    }
}
