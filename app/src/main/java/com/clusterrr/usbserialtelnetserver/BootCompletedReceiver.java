package com.clusterrr.usbserialtelnetserver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialPort;

public class BootCompletedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(context.getString(R.string.app_name), Context.MODE_PRIVATE);
        if ((prefs.getInt(MainActivity.SETTING_AUTOSTART, MainActivity.AUTOSTART_DISABLED) != MainActivity.AUTOSTART_DISABLED))
        {
            switch (UsbSerialTelnetService.getDeviceStatus(context)) {
                case NO_DEVICE:
                    return;
                case NO_PERMISSION:
                    Log.e(UsbSerialTelnetService.TAG, context.getString(R.string.missing_usb_device_permission));
                    new Handler(Looper.getMainLooper()).post(()
                        -> Toast.makeText(context, R.string.missing_usb_device_permission, Toast.LENGTH_LONG).show());
                case OK:
                    break;
            }

            Intent mainActivityStartIntent = new Intent(context, MainActivity.class);
            mainActivityStartIntent.setAction(intent.getAction());
            mainActivityStartIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainActivityStartIntent);
        }
    }
}