package com.example.leakageinhibitor;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = "MainActivity";

    private static final int BTEnableRequest = 0;
    private static final int BTDiscoverRequest = 1;
    TextView mBTStatus, mAvailableTxt, mPipe1StatusTV, mPipe2StatusTV;
    ImageView mBTIcon;
    Button mOnOffBtn, mDiscoverableBtn, mScanBtn, mStartConnectionBtn, mPipe1Btn, mPipe2Btn;
    ListView mScannedDevicesLst;
    Drawable goodPipe, leakingPipe;
//    Button mSendBtn;
//    EditText mEditTxt;

//    TextView mIncomingTxt;
//    StringBuilder messages;

    BluetoothConnectionService mBluetoothConnection;

    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //                      "8ce255c0-200a-11e0-ac64-0800200c9a66"

    BluetoothDevice mBTDevice;

    ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    DeviceListAdapter mDeviceListAdapter;

    BluetoothAdapter mBTAdapter;

    private void showToast(String msg)
    {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        switch(requestCode)
        {
            case BTEnableRequest:
                if(resultCode == RESULT_OK)
                {
                    mBTIcon.setImageResource(R.drawable.ic_action_on);
                    showToast("Bluetooth is on!");
                }
                else
                {
                    showToast("Couldn't turn bluetooth on :(");
                }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {

                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
            }
        }
    }


    private BroadcastReceiver BCReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_FOUND))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);
                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                mScannedDevicesLst.setAdapter(mDeviceListAdapter);
            }
        }
    };

    private BroadcastReceiver BCReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(device.getBondState() == BluetoothDevice.BOND_BONDED)
                {
                    showToast("Paired successfully!");
                    mBTDevice = device;
                }
                else if(device.getBondState() == BluetoothDevice.BOND_BONDING)
                {
                    showToast("Pairing...");
                }
                else if(device.getBondState() == BluetoothDevice.BOND_NONE)
                {
                    showToast("Pairing failed :(");
                }
            }
        }
    };

    public void startConnection(){
        startBTConnection(mBTDevice,MY_UUID_INSECURE);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid){
        Log.d(TAG, "startBTConnection: Initializing RFCOM Bluetooth Connection.");

        mBluetoothConnection.startClient(device,uuid);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
        mBTAdapter.cancelDiscovery();
        String deviceName = mBTDevices.get(i).getName();
        String deviceAddress = mBTDevices.get(i).getAddress();

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2)
        {
            mBTDevices.get(i).createBond();
            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);
        }
    }

    BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public void onReceive(Context context, Intent intent) {
            String text = intent.getStringExtra("theMessage");
//            messages.append(text + "\n");
//            mIncomingTxt.setText(messages);
//            for(;!text.equals(null);)
//            {
//                Log.i(TAG, text);
//            }

            switch(text)
            {
                case "1":
                    mPipe1Btn.setBackground(goodPipe);
                    mPipe1StatusTV.setText("Pipe 1 is in good state :)");
                    mPipe1StatusTV.setTextColor(getResources().getColor(R.color.greenColor));
                    break;

                case "0":
                    mPipe1Btn.setBackground(leakingPipe);
                    mPipe1StatusTV.setText("Warning! >> pipe 1 is leaking");
                    mPipe1StatusTV.setTextColor(Color.RED);
                    break;
            }
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(BCReceiver);
        unregisterReceiver(BCReceiver2);
    }


    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBTStatus = findViewById(R.id.BTStatus);
        mBTIcon = findViewById(R.id.BTIcon);
        mOnOffBtn = findViewById(R.id.onOffBtn);
        mDiscoverableBtn = findViewById(R.id.discoverableBtn);
        mScanBtn = findViewById(R.id.scanBtn);
        mAvailableTxt = findViewById(R.id.availableTxt);
        mScannedDevicesLst = findViewById(R.id.scannedDevicesLst);
        mStartConnectionBtn = findViewById(R.id.startConnectionBtn);
        mPipe1Btn = findViewById(R.id.pipe1Btn);
        mPipe2Btn = findViewById(R.id.pipe2Btn);
        goodPipe = getDrawable(R.drawable.indicator);
        leakingPipe = getDrawable(R.drawable.indicator2);
        mPipe1StatusTV = findViewById(R.id.pipe1StatusTV);
        mPipe2StatusTV = findViewById(R.id.pipe2StatusTV);
//        mSendBtn = findViewById(R.id.sendBtn);
//        mEditTxt = findViewById(R.id.editTxt);
//        mIncomingTxt = findViewById(R.id.incomingTxt);
//        messages = new StringBuilder();

        LocalBroadcastManager.getInstance(this).registerReceiver(mReceiver, new IntentFilter("incomingMessage"));

        mBTAdapter = BluetoothAdapter.getDefaultAdapter();

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(BCReceiver2, filter);

        mScannedDevicesLst.setOnItemClickListener(MainActivity.this);

        if(mBTAdapter == null)
        {
            mBTStatus.setText("Bluetooth is not available");
        }
        else
        {
            mBTStatus.setText("Bluetooth is available");
        }

        if(mBTAdapter.isEnabled())
        {
            mBTIcon.setImageResource(R.drawable.ic_action_on);
        }
        else
        {
            mBTIcon.setImageResource(R.drawable.ic_action_off);
        }

        mOnOffBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if(!mBTAdapter.isEnabled())
                {
                    showToast("Turning bluetooth on ..!");
                    Intent intent = new Intent(mBTAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(intent, BTEnableRequest);
                }

                if(mBTAdapter.isEnabled())
                {
                    mBTAdapter.disable();
                    showToast("Bluetooth is off");
                    mBTIcon.setImageResource(R.drawable.ic_action_off);
                    mScannedDevicesLst.setAdapter(null);
                    mAvailableTxt.setText("");
                }

            }
        });

        mDiscoverableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!mBTAdapter.isDiscovering())
                {
                    showToast("Making your device discoverable ..!");
                    Intent intent = new Intent(mBTAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(intent, BTDiscoverRequest);
                }

            }
        });


        mScanBtn.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                mAvailableTxt.setText("Available devices:");

                if(mBTAdapter.isDiscovering())
                    mBTAdapter.cancelDiscovery();

                mScannedDevicesLst.setAdapter(null);
                if(mDeviceListAdapter!=null)
                {
                    mDeviceListAdapter.clear();
                    mDeviceListAdapter.notifyDataSetChanged();
                }

                checkBTPermissions();
                mBTAdapter.startDiscovery();
                IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(BCReceiver, intentFilter);

            }
        });

        mStartConnectionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startConnection();
            }
        });

//        mSendBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                byte[] bytes = mEditTxt.getText().toString().getBytes(Charset.defaultCharset());
//                mBluetoothConnection.write(bytes);
//                mEditTxt.setText("");
//            }
//        });

    }

}
