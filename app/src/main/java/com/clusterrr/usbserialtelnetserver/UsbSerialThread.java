package com.clusterrr.usbserialtelnetserver;

import android.util.Log;

import com.hoho.android.usbserial.driver.UsbSerialPort;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketException;

public class UsbSerialThread extends Thread {
    final static int WRITE_TIMEOUT = 1000;

    private UsbSerialTelnetService mUsbSerialTelnetService;
    private UsbSerialPort mSerialPort;

    public UsbSerialThread(UsbSerialTelnetService usbSerialTelnetService, UsbSerialPort serialPort) {
        mUsbSerialTelnetService = usbSerialTelnetService;
        mSerialPort = serialPort;
    }

    @Override
    public void run() {
        byte buffer[] = new byte[1024];

        try {
            while (true) {
                if (mSerialPort == null) break;
                int l = mSerialPort.read(buffer, 0);
                if (l <= 0) break; // disconnect
                mUsbSerialTelnetService.writeClients(buffer, 0, l);
            }
        }
        catch (IOException e) {
            Log.i(UsbSerialTelnetService.TAG, "Serial port: " + e.getMessage());
        }
        catch (Exception e) {
            e.printStackTrace();
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
            if (mSerialPort != null) {
                mSerialPort.close();
                mSerialPort = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}