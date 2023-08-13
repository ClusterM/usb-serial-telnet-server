package com.clusterrr.usbserialtelnetserver;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

public class UsbSerialThread extends Thread {
    final static int WRITE_TIMEOUT = 1000;

    private UsbSerialTelnetService mUsbSerialTelnetService;
    private UsbSerialPort mSerialPort;
    private Handler mHandler;

    public UsbSerialThread(UsbSerialTelnetService usbSerialTelnetService, UsbSerialPort serialPort) {
        mUsbSerialTelnetService = usbSerialTelnetService;
        mSerialPort = serialPort;
        mHandler = new Handler();
    }

    @Override
    public void run() {
        byte buffer[] = new byte[1024];

        try {
            while (true) {
                if (mSerialPort == null) break;
                // Read data
                int l = mSerialPort.read(buffer, 0);
                if (l <= 0) break; // disconnect
                // Write data
                mUsbSerialTelnetService.writeClients(buffer, 0, l);
            }
        }
        catch (IOException e) {
            Log.i(UsbSerialTelnetService.TAG, "Serial port: " + e.getMessage());
            markStopped();
        }
        catch (Exception e) {
            e.printStackTrace();
            markStopped();
        }
        close();
        Log.i(UsbSerialTelnetService.TAG, "Serial port closed");
        mUsbSerialTelnetService.stopSelf();
    }

    public void write(byte[] data) throws IOException {
        if (mSerialPort != null)
            mSerialPort.write(data, WRITE_TIMEOUT);
    }

    public void close() {
        try {
            if (mSerialPort != null)
                mSerialPort.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSerialPort = null;
    }

    private void markStopped()
    {
        SharedPreferences prefs = mUsbSerialTelnetService.getApplicationContext().getSharedPreferences(mUsbSerialTelnetService.getString(R.string.app_name), Context.MODE_PRIVATE);
        prefs.edit().putBoolean(UsbSerialTelnetService.KEY_LAST_STATE, false).commit();
    }
}
