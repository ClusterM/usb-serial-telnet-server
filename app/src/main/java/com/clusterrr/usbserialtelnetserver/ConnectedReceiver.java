package com.clusterrr.usbserialtelnetserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

public class ConnectedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(UsbSerialTelnetService.TAG, "Connected device detected");
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        if (prefs.getInt(MainActivity.SETTING_AUTOSTART, MainActivity.AUTOSTART_DISABLED) != MainActivity.AUTOSTART_DISABLED)
        {
            Intent mainActivityStartIntent = new Intent(context, MainActivity.class);
            mainActivityStartIntent.setAction(intent.getAction());
            mainActivityStartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityStartIntent);
        }
    }
}