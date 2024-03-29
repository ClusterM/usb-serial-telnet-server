package com.clusterrr.usbserialtelnetserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        if ((prefs.getInt(MainActivity.SETTING_AUTOSTART, MainActivity.AUTOSTART_DISABLED) != MainActivity.AUTOSTART_DISABLED)
            && MainActivity.isDevicePresent(context))
        {
            Intent mainActivityStartIntent = new Intent(context, MainActivity.class);
            mainActivityStartIntent.setAction(intent.getAction());
            mainActivityStartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityStartIntent);
        }
    }
}