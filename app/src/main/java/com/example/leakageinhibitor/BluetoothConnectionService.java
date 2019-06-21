package com.example.leakageinhibitor;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {

    private static final String TAG = "BluetoothChatService";
    private static final String appName = "Leakage_Inhibitor";
    private static final UUID MY_UUID_INSECURE =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    //                      "8ce255c0-200a-11e0-ac64-0800200c9a66"

    private final BluetoothAdapter mBTAdapter;
    Context mContext;

    private AcceptThread mInsecureAcceptThread;

    private ConnectThread mConnectThread;
    private BluetoothDevice mDevice;
    private UUID deviceUUID;
    ProgressDialog mProgressDialog;

    private ConnectedThread mConnectedThread;

    public BluetoothConnectionService(Context context) {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        mContext = context;
        start();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBTAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);
            } catch (IOException e) {

            }

            mServerSocket = tmp;
        }

        public void run() {

            BluetoothSocket socket = null;

            try {

                socket = mServerSocket.accept();
                Log.d(TAG, "run: Start connection is done");

            } catch (IOException e) {

            }

            if (socket != null) {
                connected(socket, mDevice);
            }

        }

        public void cancel() {
            try {
                mServerSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID uuid) {
            mDevice = device;
            deviceUUID = uuid;
        }

        public void run() {
            BluetoothSocket tmp = null;

            try {

                tmp = mDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {

            }

            mSocket = tmp;

            mBTAdapter.cancelDiscovery();

            try {
                mSocket.connect();
                Log.d(TAG, "run: Start connection is done");

            } catch (IOException e) {
                try {
                    mSocket.close();
                } catch (IOException e1) {

                }
            }

            connected(mSocket, mDevice);
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {

            }
        }

    }

    public synchronized void start() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClient(BluetoothDevice device, UUID uuid) {

        mProgressDialog = ProgressDialog.show(mContext, "Connecting Bluetooth"
                , "Please Wait...", true);

        mConnectThread = new ConnectThread(device, uuid);
        mConnectThread.start();

    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream mInStream;
        private final OutputStream mOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try{
                mProgressDialog.dismiss();
            }catch (NullPointerException e){
                e.printStackTrace();
            }

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
            mOutStream = tmpOut;

        }

        public void run(){
            byte[] buffer = new byte[1024];

            int bytes;

            while (true) {
                try {
                    bytes = mInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "your input stream" + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("theMessage", incomingMessage);
                    LocalBroadcastManager.getInstance(mContext).sendBroadcast(incomingMessageIntent);

                } catch (IOException e) {
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());

            try {
                mOutStream.write(bytes);
            } catch (IOException e) {

            }
        }

        public void cancel() {
            try {
                mSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice mDevice) {

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public void write(byte[] out) {

        ConnectedThread r;
        mConnectedThread.write(out);

    }

}
