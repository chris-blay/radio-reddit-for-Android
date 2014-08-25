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

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;

public class InternetCommunication {
    public static InputStream retrieveStream(String url) {
        DefaultHttpClient client = new DefaultHttpClient();

        HttpGet getRequest = new HttpGet(url);

        try {

           HttpResponse getResponse = client.execute(getRequest);
           final int statusCode = getResponse.getStatusLine().getStatusCode();

           if (statusCode != HttpStatus.SC_OK) {
              Log.w("InternetConnection",
                  "Error " + statusCode + " for URL " + url);
              return null;
           }

           HttpEntity getResponseEntity = getResponse.getEntity();
           return getResponseEntity.getContent();

        }
        catch (IOException e) {
           getRequest.abort();
           Log.w("InternetConnection", "Error for URL " + url, e);
        }

        return null;
     }
}
