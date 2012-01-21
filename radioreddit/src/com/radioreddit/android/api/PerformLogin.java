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

import android.net.ParseException;
import android.os.AsyncTask;
import android.util.Log;

import com.radioreddit.android.LoginActivity;

public class PerformLogin extends AsyncTask<String, Integer, Boolean> {
    private LoginActivity mLoginActivity;
    private String mUser;
    private String mModhash;
    private String mCookie;
    
    public PerformLogin(LoginActivity loginActivity) {
        mLoginActivity = loginActivity;
    }
    
    @Override
    protected Boolean doInBackground(String... params) {
        final String username = params[0];
        final String password = params[1];
        
        // Prepare POST, execute it, parse response as JSON
        JSONObject response = null;
        try {
            final HttpClient httpClient = new DefaultHttpClient();
            final HttpPost httpPost = new HttpPost("https://ssl.reddit.com/api/login/" + username);
            final List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("user", username));
            nameValuePairs.add(new BasicNameValuePair("passwd", password));
            nameValuePairs.add(new BasicNameValuePair("api_type", "json"));
            httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
            final HttpResponse httpResponse = httpClient.execute(httpPost);
            response = new JSONObject(EntityUtils.toString(httpResponse.getEntity()));
            response = response.getJSONObject("json");
        } catch (UnsupportedEncodingException e) {
            Log.i("RedditAPI", "UnsupportedEncodingException while performing login", e);
        } catch (ClientProtocolException e) {
            Log.i("RedditAPI", "ClientProtocolException while performing login", e);
        } catch (IOException e) {
            Log.i("RedditAPI", "IOException while performing login", e);
        } catch (ParseException e) {
            Log.i("RedditAPI", "ParseException while performing login", e);
        } catch (JSONException e) {
            Log.i("RedditAPI", "JSONException while performing login", e);
        }
        
        // Check for failure 
        if (response == null) {
            Log.i("RedditAPI", "Response was null while performing login");
            return false;
        }
        
        // Check for errors
        final JSONArray errors = response.optJSONArray("errors");
        if (errors == null || errors.length() > 0) {
            Log.i("RedditAPI", "Response has errors while performing login");
            return false;
        }
        
        // Check for data
        final JSONObject data = response.optJSONObject("data");
        if (data == null) {
            Log.i("RedditAPI", "Response missing data while performing login");
            return false;
        }
        
        // Get modhash and cookie from data
        mUser = username;
        mModhash = data.optString("modhash");
        mCookie = data.optString("cookie");
        if (mUser == null || mModhash == null || mCookie == null) {
            Log.i("RedditAPI", "Response missing modhash/cookie while performing login");
            return false;
        }
        
        return true;
    }
    
    @Override
    protected void onPostExecute(Boolean success) {
        if (success) {
            mLoginActivity.loginResult(true, mUser, mModhash, mCookie);
        } else {
            mLoginActivity.loginResult(false, null, null, null);
        }
    }
}
