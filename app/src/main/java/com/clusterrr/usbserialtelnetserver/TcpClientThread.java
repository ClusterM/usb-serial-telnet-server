package com.clusterrr.usbserialtelnetserver;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class TcpClientThread extends Thread {
    private UsbSerialTelnetService mUsbSerialTelnetService;
    private TcpServerThread mTcpServerThread;
    private Socket mSocket;
    private InputStream mDataInputStream;
    private OutputStream mDataOutputStream;
    private String mAddress;
    private List<Byte> mBuffer;

    public TcpClientThread(UsbSerialTelnetService usbSerialTelnetService, TcpServerThread tcpServerThread, Socket socket) throws IOException {
        mUsbSerialTelnetService = usbSerialTelnetService;
        mTcpServerThread = tcpServerThread;
        mSocket = socket;
        mDataInputStream = mSocket.getInputStream();
        mDataOutputStream = mSocket.getOutputStream();
        mAddress = mSocket.getRemoteSocketAddress().toString();
        mBuffer = new ArrayList<>();
    }

    @Override
    public void run() {
        byte buffer[] = new byte[1024];

        try {
            mDataOutputStream.write(new byte[]{(byte) 0xFF, (byte) 0xFD, (byte) 0x03}); // Do Suppress Go Ahead
            mDataOutputStream.write(new byte[]{(byte) 0xFF, (byte) 0xFB, (byte) 0x03}); // Will Suppress Go Ahead
            mDataOutputStream.write(new byte[]{(byte) 0xFF, (byte) 0xFB, (byte) 0x01}); // Will Echo
            while (true) {
                if (mDataInputStream == null) break;
                int l = mDataInputStream.read(buffer);
                if (l <= 0) break; // disconnect
                for (int i = 0; i < l; i++)
                    mBuffer.add(buffer[i]);
                proceedBuffer();
            }
        }
        catch (SocketException e) {
            Log.i(UsbSerialTelnetService.TAG, mAddress + ": " + e.getMessage());
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            if (mSocket != null)
                mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSocket = null;
        mDataInputStream = null;
        mDataOutputStream = null;
        mTcpServerThread.removeClient(this);
        Log.i(UsbSerialTelnetService.TAG, mAddress + ": stopped");
    }

    private void proceedBuffer() throws IOException {
        int len = mBuffer.size();
        int i = 0;
        byte[] output = new byte[len];
        int outputSize = 0;
        for (; i < len; i++) {
            byte b = mBuffer.get(i);
            if (b == 0) continue;
            if (b == '\n') continue;
            /*
            if ((b == '\r') && ((i >= len) || (mBuffer.get(i + 1) == '\n'))) {
                // skip \r\n
                if (true)
                    continue;
            }
             */
            if (b == (byte)0xFF) {
                if (i >= len) break;
                byte next = mBuffer.get(i + 1);
                if (next == (byte)0xFF) {
                    // just 0xFF
                    //mUsbSerialTelnetService.writePort((byte) 0xFF);
                    output[outputSize++] = (byte)0xFF;
                    i++;
                    continue;
                }
                // Command
                if (i + 1 >= len) break;
                byte cmd = next;
                byte opt = mBuffer.get(i + 2);
                Log.d(UsbSerialTelnetService.TAG, "Telnet command: CMD=" + Integer.toHexString(cmd >= 0 ? cmd : cmd + 256) + " ARG=" + Integer.toHexString(opt >= 0 ? opt : opt + 256));
                i += 2;
                continue;
            }
            // just data
            //mUsbSerialTelnetService.writePort(b);
            output[outputSize++] = b;
        }

        // Remove proceeded
        for (int j = 0; j < i; j++)
            mBuffer.remove(0);

        mUsbSerialTelnetService.writeSerialPort(output, 0, outputSize);
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int len) throws IOException {
        if (mDataOutputStream != null)
            mDataOutputStream.write(data, offset, len);
        //Log.d(UsbSerialTelnetService.TAG, "Writing " + len + " bytes to TCP");
    }

    public void close() {
        try {
            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
            if (mDataOutputStream != null) {
                mDataOutputStream.close();
                mDataOutputStream = null;
            }
            if (mDataInputStream != null)
            {
                mDataInputStream.close();
                mDataInputStream = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
