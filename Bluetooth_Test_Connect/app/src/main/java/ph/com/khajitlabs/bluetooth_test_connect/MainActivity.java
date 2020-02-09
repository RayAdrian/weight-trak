package ph.com.khajitlabs.bluetooth_test_connect;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    /** Layout Elements **/
    public TextView statusView;
    public Button connectButton;
    public TextView recView;
    public EditText sendText;
    public Button sendButton;
    public Button readButton;

    /** Initialization **/
    private static final int SCAN_ITEM = 1;
    private TumakuBLE mTumakuBLE;
    private static BluetoothDevice mDevice;
    private HM10BroadcastReceiver mBroadcastReceiver;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    public boolean isScanning = false;
    public boolean isConnecting = false;
    public boolean hasFound = false;
    public boolean HM10ReceiverIsRegistered = false;

    private static Context mContext = null;
    private Handler mHandler = new Handler() ;
    private static Activity mActivity;

    IntentFilter filterDiscover = new IntentFilter(TumakuBLE.SERVICES_DISCOVERED);
    IntentFilter filterRead = new IntentFilter(TumakuBLE.READ_SUCCESS);
    IntentFilter filterNotify = new IntentFilter(TumakuBLE.NOTIFICATION);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        HM10ReceiverIsRegistered = true;

        //Initialize BT Objects
        mActivity = this;
        mContext = this;
        mBroadcastReceiver = new HM10BroadcastReceiver();
        mTumakuBLE.resetTumakuBLE();
        mTumakuBLE = TumakuBLE.getInstance(mContext);

        //Initialize UI elements
        statusView = findViewById(R.id.statusView);
        connectButton = findViewById(R.id.connectButton);
        recView = findViewById(R.id.recView);
        sendText = findViewById(R.id.sendText);
        sendButton = findViewById(R.id.sendButton);
        readButton = findViewById(R.id.readButton);

        //Initialize Button Listeners
        connectButton.setOnClickListener(this);
        sendButton.setOnClickListener(this);
        readButton.setOnClickListener(this);

        //IntentFilter for BroadcastReceiver
        registerReceiver(mBroadcastReceiver, filterDiscover);
        registerReceiver(mBroadcastReceiver, filterNotify);
        registerReceiver(mBroadcastReceiver, filterRead);

        //Request Permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, yay! Start the Bluetooth device scan.
                } else {
                    // Alert the user that this application requires the location permission to perform the scan.
                    // Close app

                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        TumakuBLE.setup();

        if (!HM10ReceiverIsRegistered) {
            registerReceiver(mBroadcastReceiver, filterDiscover);
            registerReceiver(mBroadcastReceiver, filterNotify);
            registerReceiver(mBroadcastReceiver, filterRead);
            HM10ReceiverIsRegistered = true;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        super.onStop();
        //Make sure that there is no pending Callback
        mHandler.removeCallbacks(mStopRunnable);
        mHandler.removeCallbacks(mStopConnecting);
        if (isScanning) {
            mTumakuBLE.stopLeScan(mLeScanCallback);
        }
        configureScanning(false);
        configureConnecting(false);
        hasFound = false;

        if (HM10ReceiverIsRegistered) {
            unregisterReceiver(mBroadcastReceiver);
            HM10ReceiverIsRegistered = false;
        }
    }



    public void configureScanning(boolean flag) {
        isScanning = flag;
    }

    public void configureConnecting(boolean flag) {
        isConnecting = flag;
    }

    private void scan() {
        if (isScanning) { //stop scanning
            configureScanning(false);
            mTumakuBLE.stopLeScan(mLeScanCallback);
            if (Constant.DEBUG) Log.i("JMG", "Stop scanning (scan())");
            return;
        }
        else {
            configureScanning(true);
            if (Constant.DEBUG) Log.i("JMG", "Start scanning for BLE devices...");
            mTumakuBLE.startLeScan(mLeScanCallback);

            mHandler.postDelayed(mStopRunnable, 5000); //automatically stop LE scan after 5 seconds
        }
    }

    //Device LeScan Callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String tempname="unknown";
                    if (device.getName() != null)
                        tempname = device.getName();

                    if (tempname.equals("WEIGHTTRAK")) { //WeightTrak Device Found - stop scanning
                        configureScanning(true);
                        scan(); //stop scanning
                        mHandler.removeCallbacks(mStopRunnable);
                        hasFound = true; //WEIGHTTRAK FOUND
                    }

                    final String finalName = tempname;
                    final String finalAddress = device.getAddress();
                    if (Constant.DEBUG) Log.i("JMG", "Found new device "+ finalAddress + " --- Name: " + finalName);

                    mDevice = device;

                    connectToDevice();
                }
            };

    //Device Connecting
    private void connectToDevice () {
        if (hasFound) {
            //reset found and scanning flags
            hasFound = false;
            configureScanning(false);

            //Get device parameters and configure mTumakuBLE
            String name = mDevice.getName();
            String address = mDevice.getAddress();
            if (Constant.DEBUG) recView.setText("Message: Name: " + name + "- Address: " + address);

            //Try to connect
            mTumakuBLE.setDeviceAddress(address);
            mTumakuBLE.connect();

            configureConnecting(true);

            mHandler.postDelayed(mStopConnecting, 1000); //check if connected after 1 second
        }
        //else, wait for next LeScan Callback
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        if (id == R.id.connectButton) {
            if ( connectButton.getText().toString().equals("Connect".toString()) ) { //Connect
                statusView.setText("Status: Connecting...".toString());

                //Scan for BT device
                configureScanning(false);
                hasFound = false;
                scan();
            }

            else if ( connectButton.getText().toString().equals("Disconnect".toString()) ) { //Disconnect
                statusView.setText("Status: Disconnecting...".toString());

                mTumakuBLE.disconnect();

                statusView.setText("Status: Not Connected".toString());
                connectButton.setText("Connect".toString());
            }
        }
        else if (id == R.id.sendButton) {
            if (mTumakuBLE.isConnected()) {
                String command = sendText.getText().toString();

                byte sendArray [] = new byte[command.length()];
                for (int val = 0; val < command.length(); val++)
                    sendArray[val] = (byte)command.charAt(val);
                mTumakuBLE.write(TumakuBLE.SENSORTAG_KEY_SERVICE, TumakuBLE.SENSORTAG_KEY_DATA, sendArray);

                if (Constant.DEBUG) Log.i("JMG", "sendText: " + sendText.getText().toString());
            }
            else {
                recView.setText("Error: No Device Connected to Send To");
            }

            sendText.setText("");
        }
        else if (id == R.id.readButton) {
            if (mTumakuBLE.isConnected()) {

                mTumakuBLE.read(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA);
                //Have to receive intent before doing anything else, let onReceive handle that
            }
            else {
                recView.setText("Error: No Device Connected to Read From");
            }

            sendText.setText("");
        }
    }

    public class HM10BroadcastReceiver extends BroadcastReceiver {
        //Receiving Data from onReadCharacteristic
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TumakuBLE.READ_SUCCESS)) {
                if (Constant.DEBUG) Log.i("JMG", "READ_SUCCESS message received");
                String readValue = intent.getStringExtra(mTumakuBLE.EXTRA_VALUE);

                if (readValue == null)
                    readValue = "null";

                recView.setText("Read: " + readValue);
            }
            else if (intent.getAction().equals(TumakuBLE.NOTIFICATION)) {
                if (Constant.DEBUG) Log.i("JMG", "NOTIFY received");
                String notifValue = intent.getStringExtra(mTumakuBLE.EXTRA_VALUE);

                if (notifValue == null)
                    notifValue = "null";

                recView.setText("Notification: " + notifValue);
            }
            else if (intent.getAction().equals(TumakuBLE.SERVICES_DISCOVERED)) {
                if (Constant.DEBUG) Log.i("JMG", "Services Discovered".toString());

                mTumakuBLE.enableNotifications(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA,true);
            }
        }
    }

    //Handle automatic stop of TumakuBLE.Connect()
    private Runnable mStopConnecting = new Runnable() {
        @Override
        public void run() {
            configureConnecting(false);

            if (!(mTumakuBLE.isConnected())) {
                statusView.setText("Status: Not Connected".toString());
                connectButton.setText("Connect".toString());

                if (Constant.DEBUG) Log.i("JMG", "Connection Timeout (Runnable)");
            }
            else {
                statusView.setText("Status: Connected".toString());
                connectButton.setText("Disconnect".toString());

                mTumakuBLE.discoverServices();

                if (Constant.DEBUG) Log.i("JMG", "Connected to WEIGHTTRAK Device\nName: " + mDevice.getName().toString() + "\nAddress: " + mDevice.getAddress().toString() + "\nServices:");
            }
        }
    };

    //Handle automatic stop of LEScan
    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            mTumakuBLE.stopLeScan(mLeScanCallback);
            configureScanning(false);
            hasFound = false;
            recView.setText("Error: No Device Found".toString());

            if (Constant.DEBUG) Log.i("JMG", "Stop scanning (Runnable)");
        }
    };
}
