/*
 * Copyright 2013 Thomas Hoffmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.j4velin.wifiAutoOff;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Background service which detects SCREEN_OFF events.
 * <p/>
 * Necessary for the 'turn wifi off if screen is off' option
 */
public class ScreenChangeDetector extends Service {

    final static String SCREEN_ON_ACTION = "SCREEN_ON";
    final static String SCREEN_OFF_ACTION = "SCREEN_OFF";

    private static BroadcastReceiver br;

    @Override
    public IBinder onBind(final Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (br == null) {
            if (BuildConfig.DEBUG) Logger.log("creating screen receiver");
            br = new ScreenOffReceiver();
            IntentFilter intf = new IntentFilter();
            intf.addAction(Intent.ACTION_SCREEN_ON);
            intf.addAction(Intent.ACTION_SCREEN_OFF);
            registerReceiver(br, intf);
        }
        if (intent == null) { // service restarted
            // is display already off?
            if ((Build.VERSION.SDK_INT < 20 &&
                    !((PowerManager) getSystemService(POWER_SERVICE)).isScreenOn()) ||
                    (Build.VERSION.SDK_INT >= 20 && !APILevel20Wrapper.isScreenOn(this))) {
                sendBroadcast(new Intent(this, Receiver.class).setAction(SCREEN_OFF_ACTION));
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (BuildConfig.DEBUG) Logger.log("destroying screen receiver");
        if (br != null) {
            try {
                unregisterReceiver(br);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        br = null;
    }

    static class ScreenOffReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(final Context context, final Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                context.sendBroadcast(
                        new Intent(context, Receiver.class).setAction(SCREEN_OFF_ACTION));
            } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                final SharedPreferences prefs = Receiver.getSharedPreferences(context);
                if (!prefs.getBoolean("on_screen_on", false)) {
                    if (((KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE))
                            .inKeyguardRestrictedInputMode()) {
                        // SCREEN_ON is only send if there is no lockscreen active! Otherwise the Receiver will get USER_PRESENT
                        return;
                    }
                }
                context.sendBroadcast(
                        new Intent(context, Receiver.class).setAction(SCREEN_ON_ACTION));
            }
        }

    }
}
