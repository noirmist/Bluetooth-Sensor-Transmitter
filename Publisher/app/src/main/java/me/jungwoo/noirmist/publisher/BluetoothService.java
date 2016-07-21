package me.jungwoo.noirmist.publisher;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothService {
    // Tag for Debugging
    private static final String TAG = "BluetoothService";

    // Intent request code
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Constant for status
    private static final int STATE_NONE = 0; // we're doing nothing
    private static final int STATE_LISTEN = 1; // now listening for incoming connections
    private static final int STATE_CONNECTING = 2; // now initiating an outgoing connection
    private static final int STATE_CONNECTED = 3; // now connected to a remote device


    // RFCOMM Protocol
    private static final UUID MY_UUID =
            UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");

    // Previous Activity
    private Activity activity;

    // Bluetooth connection
    private BluetoothAdapter btAdapter;
    private ConnectThread connectThread;
    private ConnectedThread connectedThread;

    private int state;

    public BluetoothService(Activity ac) {
        activity = ac;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * Check the Bluetooth support
     * @return boolean
     */
    public boolean getDeviceState() {

        if(btAdapter == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check the enabled Bluetooth
     */
    public void enableBluetooth() {
        //Bluetooth On
        if(btAdapter.isEnabled()) {
            scanDevice(); //Scanning device
        } else {
            // Bluetooth Off
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(i, REQUEST_ENABLE_BT);
        }
    }

    /**
     * Show device list
     */
    public void scanDevice() {
        Intent serverIntent = new Intent(activity, DeviceListActivity.class);
        activity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
    }

    public void getDeviceInfo(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
        connect(device);
    }

    // Bluetooth state set
    private synchronized void setState(int state) {
        this.state = state;
    }

    // Bluetooth state get
    public synchronized int getState() {
        return state;
    }

    // Reset connectThread & connectedThread for start
    public synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    // Initialize ConnectThread, Disconnect from every device
    public synchronized void connect(BluetoothDevice device) {
        // Cancel any thread attempting to make a connection
        if (state == STATE_CONNECTING) {
            if (connectThread != null) {
                connectThread.cancel();
                connectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to connect with the given device
        connectThread = new ConnectThread(device);

        connectThread.start();
        setState(STATE_CONNECTING);
    }

    // Initialize ConnectedThread
    public synchronized void connected(BluetoothSocket socket,
                                       BluetoothDevice device) {
        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(STATE_CONNECTED);
    }

    // Thread stop
    public synchronized void stop() {

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }

        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }

        setState(STATE_NONE);
    }

    // Sending data
    public void write(byte[] out) { // Create temporary object
        ConnectedThread r; // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != STATE_CONNECTED)
                return;
            r = connectedThread;
        }
        r.write(out);
    }

    // Connection Failed or Lost
    private void connectionFailed() {
        setState(STATE_LISTEN);
    }

//    // Connection Lost
//    private void connectionLost() {
//        setState(STATE_LISTEN);
//
//    }


    // ConnectThread Declaration
    private class ConnectThread extends Thread {
        private final BluetoothSocket btSocket;
        private final BluetoothDevice btDevice;

        public ConnectThread(BluetoothDevice device) {
            btDevice = device;
            BluetoothSocket tmp = null;

            // Generate BluetoothSocket
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            btSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            // Stop Searching device
            btAdapter.cancelDiscovery();

            // Try to connect BluetoothSocket
            try {
                // Return value success || exception
                btSocket.connect();

            } catch (IOException e) {
                connectionFailed();

                // close socket
                try {
                    btSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG,"unable to close() socket during connection failure",e2);
                }

                BluetoothService.this.start();
                return;
            }

            // Reset ConnectThread class
            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            // Start ConnectedThread
            connected(btSocket, btDevice);
        }

        public void cancel() {
            try {
                btSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket socket;
        private final InputStream inputStream;
        private final OutputStream outputStream;

        public ConnectedThread(BluetoothSocket socket) {
            this.socket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get inputstream & outputstram from BluetoothSocket
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            inputStream = tmpIn;
            outputStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN connectedThread");
            byte[] buffer = new byte[1024];

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Get Value from InputStream
                    inputStream.read(buffer);

                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionFailed();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                // Write data/ Send data
                outputStream.write(buffer);

            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                socket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
