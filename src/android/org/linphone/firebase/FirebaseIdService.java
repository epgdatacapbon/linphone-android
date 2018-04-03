/*
FirebaseIdService.java
Copyright (C) 2017  Belledonne Communications, Grenoble, France

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

package org.linphone.firebase;

import android.os.AsyncTask;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.linphone.LinphonePreferences;
import org.linphone.R;
import org.linphone.UIThreadDispatcher;


public class FirebaseIdService extends FirebaseInstanceIdService {
    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        final String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        android.util.Log.i("FirebaseIdService", "[Push Notification] Refreshed token: " + refreshedToken);

        String server_url = getString(R.string.registration_server_url);
        new AsyncPost().execute(server_url, refreshedToken);

        UIThreadDispatcher.dispatch(new Runnable() {
            @Override
            public void run() {
                LinphonePreferences.instance().setPushNotificationRegistrationID(refreshedToken);
            }
        });
    }

    private static class AsyncPost extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            if (params[0] == null || params[1] == null) {
                return null;
            }
            URL url;
            try {
                url = new URL("http://" + params[0]);
            } catch (MalformedURLException e) {
                android.util.Log.i("FirebaseIdService", "[Push Notification] Invalid server URL");
                return null;
            }

            String body = "register=" + params[1];
            byte bodyByte[] = body.getBytes();

            HttpURLConnection con = null;
            try {
                con = (HttpURLConnection) url.openConnection();
                con.setDoOutput(true);
                con.setUseCaches(false);
                con.setFixedLengthStreamingMode(bodyByte.length);
                con.setRequestMethod("POST");
                OutputStream out = con.getOutputStream();
                out.write(bodyByte);
                out.flush();
                out.close();
            } catch (IOException e) {
                android.util.Log.i("FirebaseIdService", "[Push Notification] Unable to send token to server");
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
            return null;
        }
    }
}
