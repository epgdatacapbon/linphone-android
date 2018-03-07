package org.linphone.firebase;

/*
FirebaseMessaging.java
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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

import org.linphone.compatibility.Compatibility;
import org.linphone.LinphoneLauncherActivity;
import org.linphone.LinphoneManager;
import org.linphone.LinphoneService;
import org.linphone.R;
import org.linphone.UIThreadDispatcher;


public class FirebaseMessaging extends FirebaseMessagingService {
    private final static int WAKE_TIME = 10000;
    private final static int INCALL_NOTIF_ID=2;

    public FirebaseMessaging() {
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        android.util.Log.i("FirebaseMessaging","[Push Notification] Received");

        acquireWakeLock(WAKE_TIME);

        if (!LinphoneService.isReady()) {
            startService(new Intent(Intent.ACTION_MAIN).setClass(this, LinphoneService.class));
        } else if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() == 0) {
            UIThreadDispatcher.dispatch(new Runnable(){
                @Override
                public void run() {
                    if (LinphoneManager.isInstanciated() && LinphoneManager.getLc().getCallsNb() == 0){
                        LinphoneManager.getLc().setNetworkReachable(false);
                        LinphoneManager.getLc().setNetworkReachable(true);
                    }
                }
            });
        }

        sendNotification(remoteMessage.getData());
    }

    void acquireWakeLock(long timeout) {
        PowerManager mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock mWakeLock = mPowerManager.newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.PARTIAL_WAKE_LOCK,
                "FirebaseMessaging");
        mWakeLock.acquire(timeout);
    }

    void sendNotification(Map<String, String> data) {
        final String msg = data.get("msg");
        final String num = data.get("num");

        NotificationManager mNM = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNM.cancel(INCALL_NOTIF_ID); // in case of crash the icon is not removed
        Compatibility.CreateChannel(this);

        Intent notifIntent = new Intent(Intent.ACTION_CALL)
                .setData(Uri.parse("tel:" + num)).setClass(this, LinphoneLauncherActivity.class);
        PendingIntent mNotifContentIntent = PendingIntent.getActivity(
                this, 0, notifIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        
        Notification mNotif = Compatibility.createNotification(
                this, getString(R.string.incoming), msg, R.drawable.linphone_notification_icon,
                R.mipmap.ic_launcher, null, mNotifContentIntent, true, Notification.PRIORITY_HIGH);

        mNotif.defaults |= Notification.DEFAULT_SOUND | Notification.DEFAULT_LIGHTS;

        mNM.notify(INCALL_NOTIF_ID, mNotif);
    }
}
