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
        List<Byte> output = new ArrayList<>();
        for (; i < len; i++) {
            byte b = mBuffer.get(i);
            if (b == 0) continue;
            if (mRemoveLf && mLastChar == '\r' && b == '\n') {
                // remove LF if need
                mLastChar = '\n';
                continue;
            }
            if (b == (byte)0xFF) {
                if (i + 1 >= len) break;
                byte next = mBuffer.get(i + 1);
                if (next == (byte)0xFF) {
                    // just 0xFF
                    output.add((byte)0xFF);
                    mLastChar = (byte)0xFF;
                    i++;
                    continue;
                }
                // Command
                if (i + 2 >= len) break;
                byte cmd = next;
                byte opt = mBuffer.get(i + 2);
                Log.d(UsbSerialTelnetService.TAG, "Telnet command: CMD=" + (cmd >= 0 ? cmd : cmd + 256) + " ARG=" + (opt >= 0 ? opt : opt + 256));
                i += 2;
                continue;
            }
            // just data
            output.add(b);
            mLastChar = b;
        }

        // Remove proceeded
        mBuffer.subList(0, i).clear();

        // And finally write data to the port
        //mUsbSerialTelnetService.writeSerialPort(Bytes.toArray(output)); // Guava lib make app too large :(
        byte[] outputPrimitive = new byte[output.size()];
        for (i = 0; i < outputPrimitive.length; i++)
            outputPrimitive[i] = output.get(i);
        mUsbSerialTelnetService.writeSerialPort(outputPrimitive);

    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int len) throws IOException {
        if (mDataOutputStream == null) return;
        List<Byte> output = new ArrayList<>();
        for (int i = 0; i < len; i++){
            byte b = data[offset + i];
            if (b != (byte)0xFF) {
                output.add(b);
            } else {
                // Escape 0xFF
                output.add((byte)0xFF);
                output.add((byte)0xFF);
            }
        }

        // mDataOutputStream.write(Bytes.toArray(output)); // Guava lib make app too large :(
        byte[] outputPrimitive = new byte[output.size()];
        for (int i = 0; i < outputPrimitive.length; i++)
            outputPrimitive[i] = output.get(i);
        mDataOutputStream.write(outputPrimitive);
    }

    public void close() {
        try {
            if (mSocket != null) {
                mSocket.close();
            }
            if (mDataOutputStream != null) {
                mDataOutputStream.close();
            }
            if (mDataInputStream != null)
            {
                mDataInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSocket = null;
        mDataOutputStream = null;
        mDataInputStream = null;
    }

    public void setNoLocalEcho(boolean noLocalEcho) {
        mNoLocalEcho = noLocalEcho;
    }

    public void setRemoveLf(boolean removeLf) {
        mRemoveLf = removeLf;
    }
}
