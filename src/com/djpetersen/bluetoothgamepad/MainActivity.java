package com.djpetersen.bluetoothgamepad;

import com.djpetersen.bluetoothgamepad.BluetoothGamepadService;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Started when the application is run
 * @author derek
 *
 */
public class MainActivity extends ActionBarActivity {
	BluetoothGamepadService mBGService;
	BluetoothAdapter mBluetoothAdapter = null;
	private GamepadState buttonState;
	// Types for messaging
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;
	public static final String DEVICE_STATE = "device_state";
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";


	final static String derrymac_bt_address = "4C:8D:79:EB:46:8B";
	final static String SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb";
	final static String TAG = "OperationBS";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		buttonState = new GamepadState();
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent en = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(en, 1); /* bad derek */
		}

		checkScanMode();

		Log.v(TAG, "running: " + mBluetoothAdapter.getAddress());

		setContentView(R.layout.activity_main);

		final Button connectButton = (Button) findViewById(R.id.buttonConnect);
		final Button buttonA = (Button) findViewById(R.id.buttonA);
		final Button buttonB = (Button) findViewById(R.id.buttonB);
		final Button buttonUp = (Button) findViewById(R.id.buttonUp);
		final Button buttonDown = (Button) findViewById(R.id.buttonDown);
		final Button buttonLeft = (Button) findViewById(R.id.buttonLeft);
		final Button buttonRight = (Button) findViewById(R.id.buttonRight);
		final Button buttonSelect = (Button) findViewById(R.id.buttonSelect);
		final Button buttonStart = (Button) findViewById(R.id.buttonStart);
		
		((View) findViewById(R.id.container)).requestFocus();
		/* set all the buttons... should be fun */
		buttonA.setOnTouchListener(new buttonListenerThing(NesButton.A,
				buttonState));
		buttonB.setOnTouchListener(new buttonListenerThing(NesButton.B,
				buttonState));
		buttonUp.setOnTouchListener(new buttonListenerThing(NesButton.UP,
				buttonState));
		buttonDown.setOnTouchListener(new buttonListenerThing(NesButton.DOWN,
				buttonState));
		buttonLeft.setOnTouchListener(new buttonListenerThing(NesButton.LEFT,
				buttonState));
		buttonRight.setOnTouchListener(new buttonListenerThing(NesButton.RIGHT,
				buttonState));
		buttonSelect.setOnTouchListener(new buttonListenerThing(
				NesButton.SELECT, buttonState));
		buttonStart.setOnTouchListener(new buttonListenerThing(NesButton.START,
				buttonState));

		connectButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Log.v(TAG, "" + mBluetoothAdapter.getScanMode());
				if (mBGService == null) {
					Toast.makeText(getApplicationContext(), "Starting BGS",
							Toast.LENGTH_SHORT).show();
					;
					mBGService = new BluetoothGamepadService(
							getApplicationContext(), handler);
				}
				if (mBGService != null) {
					if (mBGService.getState() != BGState.CONNECTED)
						mBGService.start();
				}
			}
		});

	}

	/**
	 * Ensures visibility
	 */
	private void checkScanMode() {
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			startActivity(discoverableIntent);
		}
	}
	
	/**
	 * This allows asynchronous communication between the service and 
	 * this activity. 
	 */
	@SuppressLint("HandlerLeak") /* this isnt an issue because we don't call the activity multiple times */
	private final Handler handler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_DEVICE_NAME:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(DEVICE_NAME),
						Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_WRITE:
				/* NOP */
				break;
			case MESSAGE_READ:
				byte[] bytes = (byte[]) msg.obj;
				if (bytes[0] == 'G') {
					byte[] temp = new byte[2];
					temp[0] = buttonState.getState();
					temp[1] = '\r';
					send(temp);
				}
				break;

			case MESSAGE_STATE_CHANGE:
				BGState state = (BGState) msg.obj;
				TextView s = (TextView) findViewById(R.id.textView3);
				s.setText(state.toString());
				break;

			default:
				Log.v(TAG, "UNHANDLED");
			}
		}
	};

	/**
	 * Wrapper to make sure we don't write to a null object
	 * 
	 * @param bytes
	 */
	private void send(byte[] bytes) {
		if (mBGService == null)
			return;
		mBGService.write(bytes);
	}

	/*
	 * Everything below here is assigned to various buttons
	 */
	
	public void pressSend(View v) {
		final EditText et = (EditText) findViewById(R.id.editText1);
		/* insert 'e.t. phone home' joke */
		send(et.getText().toString().concat("$").getBytes());
	}

	public void pressEx(View v) {
		String mess = "BONDED DEVICES (not necesarily 'connected'):";
		for (BluetoothDevice dev : mBluetoothAdapter.getBondedDevices()) {

			mess += "\n\t" + dev.getName() + " " + dev.getAddress();
		}
		mess += "\nCONNECTED DEVICE:";
		mess += (mBGService == null) ? "\n\tservice not started" : "\n\t"
				+ mBGService.getDeviceName();
		mess += "\nLOCAL BT MAC ADDRESS: " + mBluetoothAdapter.getAddress();
		mess += "\nDesigned by Derek for Nate <3";
		mess += "\n© Derek 2014";
		new AlertDialog.Builder(this).setTitle("Information").setMessage(mess)
				.setIcon(android.R.drawable.ic_dialog_alert).show();
	}

	public void pressKill(View v) {
		System.exit(0);
	}
	
	public void pressDeath(View v) {
			 send("~".getBytes());
		
	}
}
