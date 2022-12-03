package com.clusterrr.usbserialtelnetserver;

import android.util.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

public class TcpServerThread extends Thread {
    private UsbSerialTelnetService mUsbSerialTelnetService;
    private ServerSocket mTcpServer;
    private List<TcpClientThread> mClients;
    private boolean mNoLocalEcho = true;
    private boolean mRemoveLf = true;

    public TcpServerThread(UsbSerialTelnetService usbSerialTelnetService, ServerSocket tcpServer) {
        mUsbSerialTelnetService = usbSerialTelnetService;
        mTcpServer = tcpServer;
        mClients = new ArrayList<>();
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (mTcpServer == null) break;
                Socket socket = mTcpServer.accept();
                Log.i(UsbSerialTelnetService.TAG, "Connected: " + socket.getRemoteSocketAddress());
                String mode = "websocket"; // breaking change; use telnet if you want telnet?
                TcpClientThread client = new TcpClientThread(mUsbSerialTelnetService, this, socket, mode);
                client.setNoLocalEcho(mNoLocalEcho);
                client.setRemoveLf(mRemoveLf);
                client.start();
                mClients.add(client);
            }
        } catch (SocketException e) {
            Log.i(UsbSerialTelnetService.TAG, "Server: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        close();
        Log.i(UsbSerialTelnetService.TAG, "Server: stopped");
    }

    public void removeClient(TcpClientThread tcpClientThread) {
        mClients.remove(tcpClientThread);
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int len) throws IOException {
        List<TcpClientThread> toRemove = new ArrayList<>();
        for (TcpClientThread client : mClients) {
            try {
                client.write(data, offset, len);
            } catch (Exception ex) {
                ex.printStackTrace();
                toRemove.add(client);
            }
        }
        for (TcpClientThread client : toRemove) {
            client.close();
            mClients.remove(client);
        }
    }

    public void close() {
        try {
            if (mTcpServer != null)
                mTcpServer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        mTcpServer = null;
        for (TcpClientThread client : mClients) {
            try {
                client.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        mClients.clear();
    }

    public void setNoLocalEcho(boolean noLocalEcho) {
        mNoLocalEcho = noLocalEcho;
    }

    public void setRemoveLf(boolean removeLf) {
        mRemoveLf = removeLf;
    }
}