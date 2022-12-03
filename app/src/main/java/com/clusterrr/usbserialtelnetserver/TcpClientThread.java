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
    private WebSocketService mWebSocketService;
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
        mWebSocketService = new WebSocketService(mDataInputStream, mDataOutputStream, usbSerialTelnetService);
        mAddress = mSocket.getRemoteSocketAddress().toString();
        mBuffer = new ArrayList<>();
    }

    @Override
    public void run() {
        mWebSocketService.start();
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public String byteArrayToHexString(byte[] bytes, int offset, int len) {
        char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[len * 2];
        for (int j = offset; j < len; j++) {
          int v = bytes[j] & 0xFF;
          hexChars[j * 2] = HEX_ARRAY[v >>> 4];
          hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
      }

    public void write(byte[] data, int offset, int len) throws IOException {
        if (mDataOutputStream == null)
            return;
        // TODO: convert data to string or send binary message on websocket?
        mWebSocketService.sendMessage(byteArrayToHexString(data, offset, len));
    }

    public void close() {
        try {
            if (mSocket != null) {
                mSocket.close();
            }
            if (mDataOutputStream != null) {
                mDataOutputStream.close();
            }
            if (mDataInputStream != null) {
                mDataInputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mSocket = null;
        mDataOutputStream = null;
        mDataInputStream = null;
        mWebSocketService = null;
    }

    public void setNoLocalEcho(boolean noLocalEcho) {
        mNoLocalEcho = noLocalEcho;
    }

    public void setRemoveLf(boolean removeLf) {
        mRemoveLf = removeLf;
    }
}
