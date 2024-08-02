package com.clusterrr.usbserialtelnetserver;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;

import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, UsbSerialTelnetService.IOnStartStopListener, AdapterView.OnItemSelectedListener {
    final static String SETTING_LOCAL_ONLY = "local_only";
    final static String SETTING_TCP_PORT = "tcp_port";
    final static String SETTING_BAUD_RATE = "baud_rate";
    final static String SETTING_DATA_BITS = "data_bits";
    final static String SETTING_STOP_BITS = "stop_bits";
    final static String SETTING_PARITY = "parity";
    final static String SETTING_NO_LOCAL_ECHO = "no_local_echo";
    final static String SETTING_REMOVE_LF = "remove_lf";
    final static String SETTING_AUTOSTART = "autostart";

    final static int AUTOSTART_DISABLED = 0;
    final static int AUTOSTART_ENABLED = 1;
    final static int AUTOSTART_CLOSE = 2;

    private UsbSerialTelnetService.ServiceBinder mServiceBinder = null;
    private Handler mHandler = new Handler();
    private boolean mNeedClose = false;
    private Button mStartButton;
    private Button mStopButton;
    private Switch mLocalOnly;
    private EditText mTcpPort;
    private EditText mBaudRate;
    private Spinner mDataBits;
    private Spinner mStopBits;
    private Spinner mParity;
    private TextView mStatus;
    private Switch mNoLocalEcho;
    private Switch mRemoveLF;
    private Spinner mAutostart;

    public boolean isStarted() {
        return mServiceBinder != null && mServiceBinder.isStarted();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(UsbSerialTelnetService.TAG, "Creating activity");
        setContentView(R.layout.activity_main);

        mStartButton = findViewById(R.id.buttonStart);
        mStopButton = findViewById(R.id.buttonStop);
        mLocalOnly = findViewById(R.id.switchLocalOnly);
        mTcpPort = findViewById(R.id.editTextTcpPort);
        mBaudRate = findViewById(R.id.editTextNumberBaudRate);
        mDataBits = findViewById(R.id.spinnerDataBits);
        mStopBits = findViewById(R.id.spinnerStopBits);
        mParity = findViewById(R.id.spinnerParity);
        mStatus = findViewById(R.id.textViewStatus);
        mNoLocalEcho = findViewById(R.id.switchNoLocalEcho);
        mRemoveLF = findViewById(R.id.switchRemoveLf);
        mAutostart = findViewById(R.id.spinnerAutostart);

        mAutostart.setOnItemSelectedListener(this);
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);

        Intent serviceIntent = new Intent(this, UsbSerialTelnetService.class);
        bindService(serviceIntent, mServiceConnection, 0); // in case if service already started

        updateSettings();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        // Start service if need
        super.onNewIntent(intent);
        if (intent == null) return;
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        String action = intent.getAction();
        Log.d(UsbSerialTelnetService.TAG, "Received intent: " + action);
        switch(action)
        {
            case UsbSerialTelnetService.ACTION_NEED_TO_START:
                if (mServiceBinder == null || !mServiceBinder.isStarted())
                    start();
                break;
            case Intent.ACTION_BOOT_COMPLETED:
            case UsbManager.ACTION_USB_DEVICE_ATTACHED:
                if (!isStarted() && prefs.getInt(SETTING_AUTOSTART, AUTOSTART_DISABLED) != AUTOSTART_DISABLED)
                {
                    mNeedClose = prefs.getInt(SETTING_AUTOSTART, AUTOSTART_DISABLED) == AUTOSTART_CLOSE;
                    start();
                }
                break;
        }
    }

    public static boolean isDevicePresent(Context context)
    {
        UsbManager manager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (!availableDrivers.isEmpty()) {
            UsbSerialDriver driver = availableDrivers.get(0);
            UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
            return connection != null;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveSettings();
    }

    @Override
    public void onClick(View view)
    {
        switch(view.getId())
        {
            case R.id.buttonStart:
                mNeedClose = false;
                start();
                break;
            case R.id.buttonStop:
                stop();
                break;
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        if (parent.getId() != R.id.spinnerAutostart) return;
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        prefs.edit()
                .putInt(SETTING_AUTOSTART, position)
                .commit();
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Unused
    }

    private void start() {
        saveSettings();

        Intent ignoreOptimization = prepareIntentForWhiteListingOfBatteryOptimization(
                this, getPackageName(), false);
        if (ignoreOptimization != null) startActivity(ignoreOptimization);

        Intent serviceIntent = new Intent(this, UsbSerialTelnetService.class);
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_LOCAL_ONLY, prefs.getBoolean(SETTING_LOCAL_ONLY, false));
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_TCP_PORT, prefs.getInt(SETTING_TCP_PORT, 2323));
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_BAUD_RATE, prefs.getInt(SETTING_BAUD_RATE, 115200));
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_DATA_BITS, prefs.getInt(SETTING_DATA_BITS, 3) + 5);
        switch (prefs.getInt(SETTING_STOP_BITS, 0)) {
            case 0:
                serviceIntent.putExtra(UsbSerialTelnetService.KEY_STOP_BITS, UsbSerialPort.STOPBITS_1);
                break;
            case 1:
                serviceIntent.putExtra(UsbSerialTelnetService.KEY_STOP_BITS, UsbSerialPort.STOPBITS_1_5);
                break;
            case 2:
                serviceIntent.putExtra(UsbSerialTelnetService.KEY_STOP_BITS, UsbSerialPort.STOPBITS_2);
                break;
        }
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_PARITY, prefs.getInt(SETTING_PARITY, 0));
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_NO_LOCAL_ECHO, prefs.getBoolean(SETTING_NO_LOCAL_ECHO, true));
        serviceIntent.putExtra(UsbSerialTelnetService.KEY_REMOVE_LF, prefs.getBoolean(SETTING_REMOVE_LF, true));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        bindService(serviceIntent, mServiceConnection, 0);
    }

    private void stop() {
        Intent serviceIntent = new Intent(this, UsbSerialTelnetService.class);
        stopService(serviceIntent);
        mServiceBinder = null;
        SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        prefs.edit().putBoolean(UsbSerialTelnetService.KEY_LAST_STATE, false).commit();
        updateSettings();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            mServiceBinder = (UsbSerialTelnetService.ServiceBinder) service;
            mServiceBinder.setOnStartStopListener(MainActivity.this);
            updateSettings();
            SharedPreferences prefs = getApplicationContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
            prefs.edit().putBoolean(UsbSerialTelnetService.KEY_LAST_STATE, isStarted()).commit();
            Log.d(UsbSerialTelnetService.TAG, "Service connected");
            // Close if autoclose enabled
            if (isStarted() && mNeedClose) {
                // Delay to prevent race condition
                mHandler.postDelayed(() -> {
                    if (mNeedClose) finish();
                }, 1000);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mServiceBinder = null;
            Log.d(UsbSerialTelnetService.TAG, "Service disconnected");
        }
    };

    @Override
    public void usbSerialServiceStarted() {
    }

    @Override
    public void usbSerialServiceStopped() {
        updateSettings();
    }

    private void saveSettings() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        int tcpPort;
        try {
            tcpPort = Integer.parseInt(mTcpPort.getText().toString());
        }
        catch (NumberFormatException e) {
            tcpPort = 2323;
        }
        int baudRate;
        try {
            baudRate = Integer.parseInt(mBaudRate.getText().toString());
        }
        catch (NumberFormatException e) {
            baudRate = 115200;
        }
        prefs.edit()
                .putBoolean(SETTING_LOCAL_ONLY, mLocalOnly.isChecked())
                .putInt(SETTING_TCP_PORT, tcpPort)
                .putInt(SETTING_BAUD_RATE, baudRate)
                .putInt(SETTING_DATA_BITS, mDataBits.getSelectedItemPosition())
                .putInt(SETTING_STOP_BITS, mStopBits.getSelectedItemPosition())
                .putInt(SETTING_PARITY, mParity.getSelectedItemPosition())
                .putBoolean(SETTING_NO_LOCAL_ECHO, mNoLocalEcho.isChecked())
                .putBoolean(SETTING_REMOVE_LF, mRemoveLF.isChecked())
                .commit();
    }

    private void updateSettings() {
        SharedPreferences prefs = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        boolean started = isStarted();
        mStartButton.setEnabled(!started);
        mStopButton.setEnabled(started);
        mLocalOnly.setEnabled(!started);
        mTcpPort.setEnabled(!started);
        mBaudRate.setEnabled(!started);
        mDataBits.setEnabled(!started);
        mStopBits.setEnabled(!started);
        mParity.setEnabled(!started);
        mNoLocalEcho.setEnabled(!started);
        mRemoveLF.setEnabled(!started);
        mLocalOnly.setChecked(prefs.getBoolean(SETTING_LOCAL_ONLY, false));
        mTcpPort.setText(String.valueOf(prefs.getInt(SETTING_TCP_PORT, 2323)));
        mBaudRate.setText(String.valueOf(prefs.getInt(SETTING_BAUD_RATE, 115200)));
        mDataBits.setSelection(prefs.getInt(SETTING_DATA_BITS, 3));
        mStopBits.setSelection(prefs.getInt(SETTING_STOP_BITS, 0));
        mParity.setSelection(prefs.getInt(SETTING_PARITY, 0));
        mNoLocalEcho.setChecked(prefs.getBoolean(SETTING_NO_LOCAL_ECHO, true));
        mRemoveLF.setChecked(prefs.getBoolean(SETTING_REMOVE_LF, true));
        mAutostart.setSelection(prefs.getInt(SETTING_AUTOSTART, AUTOSTART_DISABLED));
        if (started)
            mStatus.setText(getString(R.string.started_please_connect) + " telnet://" + (prefs.getBoolean(SETTING_LOCAL_ONLY, false) ? "127.0.0.1" : UsbSerialTelnetService.getIPAddress()) + ":"+ mTcpPort.getText());
        else
            mStatus.setText(R.string.not_started);
    }

    public static Intent prepareIntentForWhiteListingOfBatteryOptimization(Context context, String packageName, boolean alsoWhenWhiteListed) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return null;
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS) == PackageManager.PERMISSION_DENIED)
            return null;
        final WhiteListedInBatteryOptimizations appIsWhiteListedFromPowerSave = getIfAppIsWhiteListedFromBatteryOptimizations(context, packageName);
        Intent intent = null;
        switch (appIsWhiteListedFromPowerSave) {
            case WHITE_LISTED:
                if (alsoWhenWhiteListed)
                    intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                break;
            case NOT_WHITE_LISTED:
                intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).setData(Uri.parse("package:" + packageName));
                break;
            case ERROR_GETTING_STATE:
            case UNKNOWN_TOO_OLD_ANDROID_API_FOR_CHECKING:
            case IRRELEVANT_OLD_ANDROID_API:
            default:
                break;
        }
        return intent;
    }

    public enum WhiteListedInBatteryOptimizations {
        WHITE_LISTED, NOT_WHITE_LISTED, ERROR_GETTING_STATE, UNKNOWN_TOO_OLD_ANDROID_API_FOR_CHECKING, IRRELEVANT_OLD_ANDROID_API
    }

    public static WhiteListedInBatteryOptimizations getIfAppIsWhiteListedFromBatteryOptimizations(Context context, String packageName) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            return WhiteListedInBatteryOptimizations.IRRELEVANT_OLD_ANDROID_API;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return WhiteListedInBatteryOptimizations.UNKNOWN_TOO_OLD_ANDROID_API_FOR_CHECKING;
        final PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm == null)
            return WhiteListedInBatteryOptimizations.ERROR_GETTING_STATE;
        return pm.isIgnoringBatteryOptimizations(packageName) ? WhiteListedInBatteryOptimizations.WHITE_LISTED : WhiteListedInBatteryOptimizations.NOT_WHITE_LISTED;
    }
}
