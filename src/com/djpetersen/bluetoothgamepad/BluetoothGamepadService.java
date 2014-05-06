package com.djpetersen.bluetoothgamepad;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * A lot of this code was based on a BluetoothChatService file included with
 * as an example with the android SDK
 * 
 * It starts when the connect button is pressed and runs for the duration of
 * the program
 * @author derek
 *
 */
public class BluetoothGamepadService extends Service {

	public static final String TAG = "BluetoothGamepadService";
	final static String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";
	final static String NAME = "BluetoothGamepad";

	private final BluetoothAdapter adapter;
	private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private BGState state;
    
	public BluetoothGamepadService(Context context, Handler handler) {
		adapter = BluetoothAdapter.getDefaultAdapter();
		mHandler = handler;
		setState(BGState.NONE);
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private synchronized void setState(BGState state) {
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, 1,
				-1, state).sendToTarget();;
		this.state = state;
	}
	
	public synchronized BGState getState() {
		return this.state;
	}
	
	public synchronized void start() {
	        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
	        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
	        if (mAcceptThread == null) {
	            mAcceptThread = new AcceptThread();
	            mAcceptThread.start();
	        }
	        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
	        Bundle bundle = new Bundle();
	        bundle.putString(MainActivity.TOAST, "Starting Threads");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
	        Log.v(TAG, "Starting");
	        setState(BGState.LISTEN);
	}

	
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        Log.d(TAG, "connected");
        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        setState(BGState.CONNECTED);
    }
    
	private void connectionLost() {
	        setState(BGState.NONE);
	        // Send a failure message back to the Activity
	        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
	        Bundle bundle = new Bundle();
	        bundle.putString(MainActivity.TOAST, "Device connection was lost");
	        msg.setData(bundle);
	        mHandler.sendMessage(msg);
	}
	
	public String getDeviceName() {
		if(state != BGState.CONNECTED) return "Not Connected";
		return mConnectedThread.getDeviceName();
	}
	
	public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);
        if (state == BGState.CONNECTING) {
            if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        }
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(BGState.CONNECTING);
    }
    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;
        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord(NAME, UUID.fromString(SPP_UUID));
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }
        public void run() {
            Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket = null;
            while (state != BGState.CONNECTED) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                if (socket != null) {
                    synchronized (BluetoothGamepadService.this) {
                        switch (state) {
                        case LISTEN:
                        case CONNECTING:
                            connected(socket, socket.getRemoteDevice());
                            break;
                        case NONE:
                        case CONNECTED:
                            try {
                                socket.close();
                            } catch (IOException e) {
                                Log.e(TAG, "Could not close unwanted socket", e);
                            }
                            break;
                        }
                    }
                }
            }
            Log.i(TAG, "END mAcceptThread");
        }
        public void cancel() {
            Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
      
    }

    private void connectionFailed() {
        setState(BGState.LISTEN);
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
    
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;
            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(UUID.fromString(SPP_UUID));
            } catch (IOException e) {
                Log.e(TAG, "create() failed", e);
            }
            mmSocket = tmp;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");
            adapter.cancelDiscovery();
            try {

                mmSocket.connect();
            } catch (IOException e) {
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                BluetoothGamepadService.this.start();
                return;
            }
            synchronized (BluetoothGamepadService.this) {
                mConnectThread = null;
            }
            connected(mmSocket, mmDevice);
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionLost();
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
                mmOutStream.write(buffer);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1, -1, buffer)
                        .sendToTarget();
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
        
        public String getDeviceName() {
        	return mmSocket.getRemoteDevice().getName();
        }
    }
    
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != BGState.CONNECTED) return;
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }
}
