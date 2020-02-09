package ph.edu.upd.eee.weighttrack;

import android.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.series.DataPoint;
import com.roughike.bottombar.BottomBar;
import com.roughike.bottombar.OnMenuTabClickListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import ph.edu.upd.eee.weighttrack.Achievementfragment;
import ph.edu.upd.eee.weighttrack.Devicefragment;
import ph.edu.upd.eee.weighttrack.SettingsActivity;
import ph.edu.upd.eee.weighttrack.Settingsfragment;
import ph.edu.upd.eee.weighttrack.Timelinefragment;

public class MainActivity extends AppCompatActivity {

    BottomBar mBottomBar;
    private FirebaseAuth fireAuth;
    private DatabaseReference fireDb;
    private DatabaseHelper myDb;
    private String uid;

    private int measure_buf = 5; //5 measurements per measure operation
    private int measure_ind = 0; //index of current measure data
    private float measure_weight [] = new float[measure_buf];

    private static final int SCAN_ITEM = 1;
    private TumakuBLE mTumakuBLE;
    private static BluetoothDevice mDevice;
    private HM10BroadcastReceiver mBroadcastReceiver;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 456;

    public boolean isScanning = false;
    public boolean HM10ReceiverIsRegistered = false;

    private static Context mContext = null;
    private Handler mHandler = new Handler() ;
    private static Activity mActivity;

    IntentFilter filterDiscover = new IntentFilter(TumakuBLE.SERVICES_DISCOVERED);
    IntentFilter filterRead = new IntentFilter(TumakuBLE.READ_SUCCESS);
    IntentFilter filterNotify = new IntentFilter(TumakuBLE.NOTIFICATION);

    public Button btn_connect;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fireAuth = FirebaseAuth.getInstance();
        uid = fireAuth.getCurrentUser().getUid();
        fireDb = FirebaseDatabase.getInstance().getReference();
        myDb = new DatabaseHelper(this);

        mBottomBar = BottomBar.attach(this,savedInstanceState);
        mBottomBar.setItemsFromMenu(R.menu.menu_main, new OnMenuTabClickListener() {
            @Override
            public void onMenuTabSelected(int menuItemId) {
                if(menuItemId == R.id.bottombaritemone){
                    Timelinefragment f = new Timelinefragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame,f).commit();
                }
                else if(menuItemId == R.id.bottombaritemtwo){
                    Devicefragment f = new Devicefragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame,f).commit();
                }
                else if(menuItemId == R.id.bottombaritemthree){
                    Achievementfragment f = new Achievementfragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame,f).commit();
                }
                else if(menuItemId == R.id.bottombaritemfour){
                    Settingsfragment f = new Settingsfragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame,f).commit();
                }

            }

            @Override
            public void onMenuTabReSelected(int menuItemId) {

            }

        });

        HM10ReceiverIsRegistered = true;

        //Initialize BT Objects
        mActivity = this;
        mContext = this;
        mBroadcastReceiver = new HM10BroadcastReceiver();
        mTumakuBLE.resetTumakuBLE();
        mTumakuBLE = TumakuBLE.getInstance(mContext);

        //Broadcast Receivers for TumakuBLE
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

        if (HM10ReceiverIsRegistered) {
            unregisterReceiver(mBroadcastReceiver);
            HM10ReceiverIsRegistered = false;
        }
    }



    public void onMeasureClick(View v){
        if (mTumakuBLE.isConnected()) {
            /**
            String command = "M" + measure_buf; //Measure weight measure_buf times (default: 5)
            byte sendArray [] = new byte[command.length()];
            for (int val = 0; val < command.length(); val++)
                sendArray[val] = (byte)command.charAt(val);
            mTumakuBLE.write(TumakuBLE.SENSORTAG_KEY_SERVICE, TumakuBLE.SENSORTAG_KEY_DATA, sendArray);

            //wait for notify event for data
             **/
            mTumakuBLE.read(TumakuBLE.SENSORTAG_KEY_SERVICE, TumakuBLE.SENSORTAG_KEY_DATA);

            //if (Constant.DEBUG) Log.i("JMG", "command: " + command);
        }
        else { //no device connected
            Toast.makeText(MainActivity.this, "No device connected", Toast.LENGTH_SHORT).show();
        }
    }

    public void onSettingsClick(View v){
        Intent myIntent = new Intent(getBaseContext(),   SettingsActivity.class);
        startActivity(myIntent);
    }

    public void onSignoutClick(View v) {
        Log.d("MainActivity","onSignoutClick");
        fireAuth.signOut();
        Log.d("MainActivity","onSignoutClick:successful");
        Toast.makeText(MainActivity.this, "Signed out", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(MainActivity.this, LoginActivity.class));
    }

    public void onConnectClick(View v) { //Connect to BLE
        if (!(mTumakuBLE.isConnected())) { //not connected, so connect
            //Scan for BT device
            configureScanning(false);
            scan();

            //Wait for callbacks to LeScan (LeScanCallback)
        }
        else { //connected, so disconnect
            mTumakuBLE.disconnect();
            //btn_connect.setText("Connect".toString());
        }

    }

    public void configureScanning(boolean flag) {
        isScanning = flag;
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

            mHandler.postDelayed(mStopRunnable, 8000); //automatically stop LE scan after 8s seconds
        }
    }

    //Device LeScan Callback
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                    String tempname = "unknown";
                    if (device.getName() != null)
                        tempname = device.getName();

                    final String finalName = tempname;
                    final String finalAddress = device.getAddress().toString();
                    if (Constant.DEBUG) Log.i("JMG", "Found new device "+ finalAddress + " --- Name: " + finalName);

                    mDevice = device;

                    if (tempname.equals("WEIGHTTRAK")) { //WeightTrak Device Found - stop scanning
                        //Stop Scanning
                        configureScanning(true);
                        scan(); //stop scanning
                        mHandler.removeCallbacks(mStopRunnable);
                        configureScanning(false);

                        //Get device parameters and configure mTumakuBLE
                        String name = mDevice.getName();
                        String address = mDevice.getAddress();

                        //Try to connect
                        mTumakuBLE.setDeviceAddress(address);
                        mTumakuBLE.connect();

                        mHandler.postDelayed(mStopConnecting, 2000); //check if connected after 2 second
                    }
                }
            };

    public class HM10BroadcastReceiver extends BroadcastReceiver {
        @Override
        //Receiving Data from onReadCharacteristic
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(TumakuBLE.READ_SUCCESS)) {
                if (Constant.DEBUG) Log.i("JMG", "READ_SUCCESS message received");
                String readValue = intent.getStringExtra(mTumakuBLE.EXTRA_VALUE);

                if (readValue == null)
                    readValue = "null";

                String currentDatetime = Calendar.getInstance().getTime().toString();
                boolean isInserted = myDb.insertData( currentDatetime, readValue );
                fireDb.child("user-entries").child(uid).child(currentDatetime).setValue(readValue);
                Log.d("MainActivity","onMeasureClick:"+isInserted);
                if(isInserted)
                    Toast.makeText(MainActivity.this,"Data Inserted",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this,"Data Not Inserted",Toast.LENGTH_LONG).show();

            }
            //Receiving Data from onChangeCharacteristic
            else if (intent.getAction().equals(TumakuBLE.NOTIFICATION)) {
                if (Constant.DEBUG) Log.i("JMG", "NOTIFY received");

                String notifValue = intent.getStringExtra(mTumakuBLE.EXTRA_VALUE);

                if (notifValue == null)
                    notifValue = "null";

                measure_weight[measure_ind] = Float.valueOf(notifValue);
                measure_ind++;

                Log.i("JMG","measure_weight: " + measure_weight[measure_ind]);
                Log.i("JMG","measure_ind: " + measure_ind);


                float weight_ave = 0;
                if (measure_ind == measure_buf) {
                    for (int i = 0; i < measure_buf; i++)
                        weight_ave += measure_weight[i];
                    weight_ave = weight_ave/measure_buf;
                    measure_ind = 0;
                }

                String readValue = Float.toString(weight_ave);

                String currentDatetime = Calendar.getInstance().getTime().toString();
                boolean isInserted = myDb.insertData( currentDatetime, readValue );
                fireDb.child("user-entries").child(uid).child(currentDatetime).setValue(readValue);
                Log.d("MainActivity","onMeasureClick:"+isInserted);
                if(isInserted)
                    Toast.makeText(MainActivity.this,"Data Inserted",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(MainActivity.this,"Data Not Inserted",Toast.LENGTH_LONG).show();
            }
            else if (intent.getAction().equals(TumakuBLE.SERVICES_DISCOVERED)) {
                if (Constant.DEBUG) Log.i("JMG", "Services Discovered".toString());

                mTumakuBLE.enableNotifications(TumakuBLE.SENSORTAG_KEY_SERVICE,TumakuBLE.SENSORTAG_KEY_DATA,true);

                mTumakuBLE.read(TumakuBLE.SENSORTAG_KEY_SERVICE, TumakuBLE.SENSORTAG_KEY_DATA);
            }
        }
    }

    //Handle automatic stop of TumakuBLE.Connect()
    private Runnable mStopConnecting = new Runnable() {
        @Override
        public void run() {
            if (!(mTumakuBLE.isConnected())) {
                if (Constant.DEBUG) Log.i("JMG", "Connection Timeout (Runnable)");

            }
            else {
                mTumakuBLE.discoverServices();

                //if (Constant.DEBUG) Log.i("JMG", "Connected to WEIGHTTRAK Device\nName: " + mDevice.getName().toString() + "\nAddress: " + mDevice.getAddress().toString() + "\nServices:");

                //btn_connect.setText("Disconnect".toString());
            }
        }
    };

    //Handle automatic stop of LEScan
    private Runnable mStopRunnable = new Runnable() {
        @Override
        public void run() {
            mTumakuBLE.stopLeScan(mLeScanCallback);
            configureScanning(false);

            if (Constant.DEBUG) Log.i("JMG", "Stop scanning (Runnable)");
        }
    };

}
