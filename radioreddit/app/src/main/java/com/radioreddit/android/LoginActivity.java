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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.radioreddit.android.actionbarcompat.ActionBarActivity;
import com.radioreddit.android.api.LoginResultCallback;
import com.radioreddit.android.api.RedditApi;

public class LoginActivity extends ActionBarActivity implements LoginResultCallback {
    private Context mContext;
    private EditText mUsername;
    private EditText mPassword;
    private MusicService mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = ((MusicService.MusicBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        mContext = getApplicationContext();
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);

        bindService(
                new Intent(mContext, MusicService.class), mConnection, Context.BIND_AUTO_CREATE);

        findViewById(R.id.login).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });

        setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(mConnection);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // Finish this activity.
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void login() {
        // Get username/password.
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();

        // Check username/password.
        if (username.isEmpty()) {
            toast(R.string.no_username);
            return;
        }
        if (password.isEmpty()) {
            toast(R.string.no_password);
            return;
        }

        RedditApi.requestLogin(this, username, password);
    }

    @Override
    public void onLoginResult(boolean success, String username, String modhash, String cookie) {
        if (success) {
            mService.login(username, modhash, cookie);
            finish();
        } else {
           toast(R.string.bad_login);
        }
    }

    private void toast(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }
}
