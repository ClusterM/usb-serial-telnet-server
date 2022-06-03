package com.clusterrr.usbserialtelnetserver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.List;

public class UsbSerialTelnetService extends Service {
    final static String TAG = "UsbSerialTelnet";
    final static String ACTION_NEED_TO_START = "need_to_start";
    final static String KEY_TCP_PORT = "tcp_port";
    final static String KEY_BAUD_RATE = "baud_rate";
    final static String KEY_DATA_BITS = "data_bits";
    final static String KEY_STOP_BITS = "stop_bits";
    final static String KEY_PARITY = "parity";
    final static String KEY_NO_LOCAL_ECHO = "no_local_echo";
    final static String KEY_REMOVE_LF = "remove_lf";

    boolean mStarted = false;
    //UsbSerialPort mSerialPort = null;
    UsbSerialThread mUsbSerialThread = null;
    TcpServerThread mTcpServerThread = null;

    int mTcpPort = 2323;

    public UsbSerialTelnetService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (mStarted) {
            // Already started
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(UsbSerialTelnetService.this.getApplicationContext(), getString(R.string.already_started), Toast.LENGTH_LONG).show());
            return START_STICKY;
        }

        String message = getString(R.string.app_name) + " " + getString(R.string.started);
        boolean success = false;

        try {
            // Find all available drivers from attached devices.
            UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
            List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
            if (availableDrivers.isEmpty()) {
                message = getString(R.string.device_not_found);
            } else {
                // Open a connection to the first available driver.
                UsbSerialDriver driver = availableDrivers.get(0);
                UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
                if (connection == null) {
                    message = null; // "Please grant permission and try again";
                    Intent mainActivityStartIntent = new Intent(this, MainActivity.class);
                    mainActivityStartIntent.setAction(ACTION_NEED_TO_START);
                    PendingIntent mainActivityStartPendingIntent = PendingIntent.getActivity(this, 0, mainActivityStartIntent, PendingIntent.FLAG_IMMUTABLE);
                    manager.requestPermission(driver.getDevice(), mainActivityStartPendingIntent);
                } else {
                    UsbSerialPort serialPort = driver.getPorts().get(0); // Most devices have just one port (port 0)
                    serialPort.open(connection);
                    serialPort.setParameters(
                            intent.getIntExtra(KEY_BAUD_RATE, 115200),
                            intent.getIntExtra(KEY_DATA_BITS, 8),
                            intent.getIntExtra(KEY_STOP_BITS, UsbSerialPort.STOPBITS_1),
                            intent.getIntExtra(KEY_PARITY, UsbSerialPort.PARITY_NONE));
                    ServerSocket serverSocket = new ServerSocket(intent.getIntExtra(KEY_TCP_PORT,2323));
                    mUsbSerialThread = new UsbSerialThread(this, serialPort);
                    mTcpServerThread = new TcpServerThread(this, serverSocket);
                    mTcpServerThread.setNoLocalEcho(intent.getBooleanExtra(KEY_NO_LOCAL_ECHO, true));
                    mTcpServerThread.setRemoveLf(intent.getBooleanExtra(KEY_REMOVE_LF, true));
                    mUsbSerialThread.start();
                    mTcpServerThread.start();
                    success = true;
                }
            }
        }
        catch (Exception ex) {
            message = getString(R.string.error) + " " + ex.getMessage();
            ex.printStackTrace();
        }

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        PendingIntent mainActivityPendingIntent = PendingIntent.getActivity(this, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE);
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(TAG,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.app_name));
            nm.createNotificationChannel(channel);
        }
        Notification notification = new NotificationCompat.Builder(this, TAG)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(this.getResources(), R.mipmap.ic_launcher))
                .setContentTitle(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setShowWhen(false)
                .setContentIntent(mainActivityPendingIntent)
                .build();

        if (message != null) {
            final String msg = message;
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(UsbSerialTelnetService.this.getApplicationContext(), msg, Toast.LENGTH_SHORT).show());
        }

        startForeground(1, notification);

        if (success) {
            if (message != null)
                Log.i(TAG, message);
            mStarted = true;
        } else {
            if (message != null)
                Log.e(TAG, message);
            stopSelf();
            mStarted = false;
        }
        return START_STICKY;
    }

    public static String getIPAddress() {
        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                if (networkInterface.isUp()) {
                    String name = networkInterface.getName();
                    if (name.toLowerCase().equals("wlan0") || name.toLowerCase().equals("rmnet0")) {
                        List<InterfaceAddress> interfaceAddresses = networkInterface.getInterfaceAddresses();
                        for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                            InetAddress address = interfaceAddress.getAddress();
                            if (address instanceof Inet4Address){
                                return address.getHostAddress();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "localhost";
    }

    @Override
    public void onDestroy()
    {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.cancel(1);
        if (mTcpServerThread != null) {
            mTcpServerThread.close();
            mTcpServerThread = null;
        }
        if (mUsbSerialThread != null) {
            mUsbSerialThread.close();
            mUsbSerialThread = null;
        }
        if (mStarted)
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(UsbSerialTelnetService.this.getApplicationContext(),
                    getString(R.string.app_name) + " " + getString(R.string.stopped), Toast.LENGTH_SHORT).show());
        Log.i(TAG, "Service stopped");
        mStarted = false;
        mBinder.stopped();
    }

    private final ServiceBinder mBinder = new ServiceBinder();
    public class ServiceBinder extends Binder {
        private IOnStopListener onStopListener = null;
        public boolean isStarted()
        {
            return mStarted;
        }
        public void setOnStopListener(IOnStopListener listener) { onStopListener = listener; }
        public void stopped() { if (onStopListener != null) onStopListener.usbSerialServiceStopped(); }
    }
    public interface IOnStopListener
    {
        public void usbSerialServiceStopped();
    }

    public void writeSerialPort(byte[] buffer) throws IOException {
        if (mUsbSerialThread == null) return;
        mUsbSerialThread.write(buffer);
    }

    public void writeSerialPort(byte[] buffer, int pos, int len) throws IOException {
        if (mUsbSerialThread == null) return;
        if ((pos != 0) || (buffer.length != len)) {
            byte[] writeBuffer = new byte[len];
            System.arraycopy(buffer, pos, writeBuffer, 0, len);
            mUsbSerialThread.write(writeBuffer);
        } else {
            mUsbSerialThread.write(buffer);
        }
    }

    public void writeClients(byte[] buffer) throws IOException {
        if (mTcpServerThread == null) return;
        mTcpServerThread.write(buffer);
    }

    public void writeClients(byte[] buffer, int pos, int len) throws IOException {
        if (mTcpServerThread == null) return;
        mTcpServerThread.write(buffer, pos, len);
    }
}
