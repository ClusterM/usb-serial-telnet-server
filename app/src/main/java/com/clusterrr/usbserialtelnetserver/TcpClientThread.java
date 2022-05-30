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
    private boolean mNoLocalEcho = true;
    private boolean mRemoveLf = true;
    private byte mLastChar = 0;

    final static byte CMD_WILL = (byte) 0xFB;
    final static byte CMD_WONT = (byte) 0xFC;
    final static byte CMD_DO = (byte) 0xFD;
    final static byte CMD_DONT = (byte) 0xFE;
    final static byte OP_ECHO = (byte) 1;
    final static byte OP_SUPPRESS = (byte) 3;

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
            if (mNoLocalEcho) {
                mDataOutputStream.write(new byte[]{(byte) 0xFF, CMD_WILL, OP_ECHO}); // Will Echo
                mDataOutputStream.write(new byte[]{(byte) 0xFF, CMD_DONT, OP_ECHO}); // Don't Echo
                mDataOutputStream.write(new byte[]{(byte) 0xFF, CMD_DO, OP_SUPPRESS}); // Do Suppress Go Ahead
                mDataOutputStream.write(new byte[]{(byte) 0xFF, CMD_WILL, OP_SUPPRESS}); // Will Suppress Go Ahead
            }

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
            if (mRemoveLf && mLastChar == '\r' && b == '\n') {
                // remove LF if need
                mLastChar = '\n';
                continue;
            }
            if (b == (byte)0xFF) {
                if (i >= len) break;
                byte next = mBuffer.get(i + 1);
                if (next == (byte)0xFF) {
                    // just 0xFF
                    output[outputSize++] = mLastChar = (byte)0xFF;
                    i++;
                    continue;
                }
                // Command
                if (i + 1 >= len) break;
                byte cmd = next;
                byte opt = mBuffer.get(i + 2);
                Log.d(UsbSerialTelnetService.TAG, "Telnet command: CMD=" + (cmd >= 0 ? cmd : cmd + 256) + " ARG=" + (opt >= 0 ? opt : opt + 256));
                i += 2;
                continue;
            }
            // just data
            output[outputSize++] = mLastChar = b;
            mLastChar = b;
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

    public void setNoLocalEcho(boolean noLocalEcho) {
        mNoLocalEcho = noLocalEcho;
    }

    public void setRemoveLf(boolean removeLf) {
        mRemoveLf = removeLf;
    }
}
