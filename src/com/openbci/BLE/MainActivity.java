package com.openbci.BLE;

import java.util.Arrays;
import java.util.UUID;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements LeScanCallback {

	private final String TAG = MainActivity.class.getSimpleName();
	// Bluetooth State
	final private static int STATE_BLUETOOTH_OFF = 1;
	final private static int STATE_DISCONNECTED = 2;
	final private static int STATE_CONNECTING = 3;
	final private static int STATE_CONNECTED = 4;

	private int mBluetoothState;
	private boolean mScanStarted;
	private boolean mScanning;

	private BluetoothAdapter mBluetoothAdapter;
	private BluetoothDevice mBluetoothDevice;

	private RFduinoService mRFduinoService;
	private Button mEnableBluetoothButton;
	private TextView mScanStatusText;
	private Button mScanButton;
	private TextView mDeviceInfoText;
	private TextView mConnectionStatusText;
	private Button mConnectButton;
	private EditText mCommandText;
	private Button mSendButton;

	private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);

			if (state == BluetoothAdapter.STATE_ON) {
				upgradeState(STATE_DISCONNECTED);
			} else if (state == BluetoothAdapter.STATE_OFF) {
				downgradeState(STATE_BLUETOOTH_OFF);
			}

		}
	};

	private final BroadcastReceiver scanModeReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mScanning = (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_NONE);
			mScanStarted &= mScanning;
			updateUi();
		}
	};

	private final BroadcastReceiver rfduinoReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			if (RFduinoService.ACTION_CONNECTED.equals(action)) {
				upgradeState(STATE_CONNECTED);
			} else if (RFduinoService.ACTION_DISCONNECTED.equals(action)) {
				downgradeState(STATE_DISCONNECTED);
			} else if (RFduinoService.ACTION_DATA_AVAILABLE.equals(action)) {
				byte[] data = intent
						.getByteArrayExtra(RFduinoService.EXTRA_DATA);
				// displayReceived(data);
				byte[] packetData = Arrays.copyOfRange(data, 1, data.length);
				int packetNumber = Byte.valueOf(data[0]).intValue();
				Log.i(TAG, "RFDuino Data: " +packetNumber+ ": " +HexAsciiHelper.bytesToAsciiMaybe(packetData));
				new SaveBytesToFile().execute(data);
			}

		}
	};

	private final ServiceConnection rfduinoServiceConnection = new ServiceConnection() {

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mRFduinoService = null;
			downgradeState(STATE_DISCONNECTED);
		}

		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mRFduinoService = ((RFduinoService.LocalBinder) service)
					.getService();
			if (mRFduinoService.initialize()) {
				if (mRFduinoService.connect(mBluetoothDevice.getAddress())) {
					upgradeState(STATE_CONNECTING);
				}
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		// Bluetooth
		mEnableBluetoothButton = (Button) findViewById(R.id.enableBluetooth);
		mEnableBluetoothButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				if (mBluetoothState == STATE_BLUETOOTH_OFF) {
					AlertDialog.Builder confirmOnBluetooth = new Builder(
							MainActivity.this);
					confirmOnBluetooth.setTitle("Turn on Bluetooth?");
					confirmOnBluetooth.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									mEnableBluetoothButton.setText(mBluetoothAdapter
											.enable() ? "Enabling Bluetooth"
											: "Enable Failed!");
								}
							});
					confirmOnBluetooth.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
					confirmOnBluetooth.show();
				} else {

					AlertDialog.Builder confirmOffBluetooth = new Builder(
							MainActivity.this);
					String title = mBluetoothState > 2 ? "Bluetooth is being accessed by a device. Sure you want to turn it off?"
							: "Turn Off Bluetooth?";
					confirmOffBluetooth.setTitle(title);
					confirmOffBluetooth.setPositiveButton("Yes",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									mEnableBluetoothButton.setText(mBluetoothAdapter
											.disable() ? "Disabling Bluetooth"
											: "Disable Failed!");
								}
							});
					confirmOffBluetooth.setNegativeButton("No",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int which) {
									dialog.cancel();
								}
							});
					confirmOffBluetooth.show();
				}

			}
		});

		// Scan for RFDuino
		mScanStatusText = (TextView) findViewById(R.id.scanStatus);

		mScanButton = (Button) findViewById(R.id.scan);
		mScanButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mBluetoothAdapter.startLeScan(
						new UUID[] { RFduinoService.UUID_SERVICE },
						MainActivity.this);
			}
		});

		// Connected RFDuino Info
		mDeviceInfoText = (TextView) findViewById(R.id.deviceInfo);

		// Connect to RFDuino
		mConnectionStatusText = (TextView) findViewById(R.id.connectionStatus);
		mConnectButton = (Button) findViewById(R.id.connect);
		mConnectButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				v.setEnabled(false);
				mConnectionStatusText.setText("Connecting...");
				Intent rfduinoIntent = new Intent(MainActivity.this,
						RFduinoService.class);
				bindService(rfduinoIntent, rfduinoServiceConnection,
						BIND_AUTO_CREATE);
			}
		});

		// Send
		mCommandText = (EditText) findViewById(R.id.sendValue);
		mSendButton = (Button) findViewById(R.id.sendButton);
		mSendButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				mRFduinoService.send(mCommandText.getText().toString()
						.getBytes());
			}
		});
	}

	protected void onStart() {
		super.onStart();

		registerReceiver(scanModeReceiver, new IntentFilter(
				BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
		registerReceiver(bluetoothStateReceiver, new IntentFilter(
				BluetoothAdapter.ACTION_STATE_CHANGED));
		registerReceiver(rfduinoReceiver, RFduinoService.getIntentFilter());

		updateState(mBluetoothAdapter.isEnabled() ? STATE_DISCONNECTED
				: STATE_BLUETOOTH_OFF);
	}

	@Override
	protected void onStop() {
		super.onStop();

		mBluetoothAdapter.stopLeScan(this);

		unregisterReceiver(scanModeReceiver);
		unregisterReceiver(bluetoothStateReceiver);
		unregisterReceiver(rfduinoReceiver);
	}

	private void upgradeState(int newState) {
		if (newState > mBluetoothState) {
			updateState(newState);
		}
	}

	private void downgradeState(int newState) {
		if (newState < mBluetoothState) {
			updateState(newState);
		}
	}

	private void updateState(int newState) {
		mBluetoothState = newState;
		updateUi();
	}

	private void updateUi() {
		// Enable Bluetooth
		boolean on = mBluetoothState > STATE_BLUETOOTH_OFF;
		// mEnableBluetoothButton.setEnabled(!on);
		mEnableBluetoothButton.setText(on ? "Disable Bluetooth"
				: "Enable Bluetooth");
		mScanButton.setEnabled(on);

		// Scan
		if (mScanStarted && mScanning) {
			mScanStatusText.setText("Scanning...");
			mScanButton.setText("Stop Scanning");
			mScanButton.setEnabled(true);
		} else if (mScanStarted) {
			mScanStatusText.setText("Scan started...");
			mScanButton.setEnabled(false);
		} else {
			mScanStatusText.setText("");
			mScanButton.setText("Scan");
			mScanButton.setEnabled(on);
		}

		// Connect
		boolean connected = false;
		String connectionText = "Disconnected";
		if (mBluetoothState == STATE_CONNECTING) {
			connectionText = "Connecting...";
		} else if (mBluetoothState == STATE_CONNECTED) {
			connected = true;
			connectionText = "Connected";
		}
		mConnectionStatusText.setText(connectionText);
		mConnectButton.setEnabled(mBluetoothDevice != null
				&& mBluetoothState == STATE_DISCONNECTED);

		mCommandText.setEnabled(connected);
		mSendButton.setEnabled(connected);

	}

	private void displayReceived(byte[] data) {
		AlertDialog.Builder receivedAlert = new Builder(MainActivity.this);
		String asciiData = HexAsciiHelper.bytesToAsciiMaybe(data);
		receivedAlert.setTitle("Received:");
		receivedAlert.setMessage(asciiData);
		receivedAlert.show();
	}

	@Override
	public void onLeScan(BluetoothDevice device, final int rssi,
			final byte[] scanRecord) {
		mBluetoothAdapter.stopLeScan(this);
		mBluetoothDevice = device;

		MainActivity.this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mDeviceInfoText.setText(BluetoothHelper.getDeviceInfoText(
						mBluetoothDevice, rssi, scanRecord));
				updateUi();
			}
		});

	}

}
