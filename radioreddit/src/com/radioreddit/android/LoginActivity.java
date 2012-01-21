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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.radioreddit.android.actionbarcompat.ActionBarActivity;
import com.radioreddit.android.api.RedditApi;

public class LoginActivity extends ActionBarActivity {
    private Context mContext;
    private EditText mUsername;
    private EditText mPassword;
    private Button mLogin;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        mContext = getApplicationContext();
        mUsername = (EditText) findViewById(R.id.username);
        mPassword = (EditText) findViewById(R.id.password);
        mLogin = (Button) findViewById(R.id.login);
        
        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        
        setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // finish this activity
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private void login() {
        // get username/password
        final String username = mUsername.getText().toString();
        final String password = mPassword.getText().toString();
        
        // check username/password
        if (username.length() == 0) {
            toast(R.string.no_username);
            return;
        }
        if (password.length() == 0) {
            toast(R.string.no_password);
            return;
        }
        
        RedditApi.requestLogin(this, username, password);
    }
    
    public void loginResult(boolean success, String user, String modhash, String cookie) {
        if (success) {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            final SharedPreferences.Editor editor = preferences.edit();
            editor.putString(MainActivity.PREF_USER, user);
            editor.putString(MainActivity.PREF_MODHASH, modhash);
            editor.putString(MainActivity.PREF_COOKIE, cookie);
            editor.commit();
            toast(getString(R.string.now_logged_in_as) + " " + user);
            finish();
        } else {
            toast(R.string.bad_login);
        }
    }
    
    private void toast(int resId) {
        Toast.makeText(mContext, resId, Toast.LENGTH_SHORT).show();
    }
    
    private void toast(String text) {
        Toast.makeText(mContext, text, Toast.LENGTH_SHORT).show();
    }
}
