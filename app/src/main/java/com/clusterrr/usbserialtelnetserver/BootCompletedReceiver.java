package com.clusterrr.usbserialtelnetserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // App was started before shutdown
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        boolean needToStart = prefs.getBoolean(UsbSerialTelnetService.KEY_LAST_STATE, false);
        if (needToStart) {
            Intent mainActivityStartIntent = new Intent(context, MainActivity.class);
            context.startActivity(mainActivityStartIntent);
        }
    }
}