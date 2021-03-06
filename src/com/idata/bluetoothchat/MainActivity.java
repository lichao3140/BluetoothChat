package com.idata.bluetoothchat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @description:此类为蓝牙调试助手主界面 接收发送蓝牙消息、建立蓝牙通信连接、打印蓝牙消息
 */
public class MainActivity extends Activity implements OnClickListener {

	// 从BluetoothChatService发送处理程序的消息类型
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private TextView mTitle;
	private ListView mConversationView;
	private Button mSendButton;
	private Button mClearButton;

	// 连接设备的名称
	private String mConnectedDeviceName = null;
	private ArrayAdapter<String> mConversationArrayAdapter;

	// 本地蓝牙适配器
	private BluetoothAdapter mBluetoothAdapter = null;
	// 成员对象聊天服务
	private BluetoothChatService mChatService = null;
	private Button btn_connect, btn_discover;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTitle = (TextView) findViewById(R.id.title_left_text);
		mTitle.setText(R.string.app_name);
		mTitle = (TextView) findViewById(R.id.title_right_text);
		btn_connect = (Button) findViewById(R.id.btn_connect);
		btn_discover = (Button) findViewById(R.id.btn_discover);
		btn_connect.setOnClickListener(this);
		btn_discover.setOnClickListener(this);
		// 获取本地蓝牙适配器
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		// 判断蓝牙是否可用
		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "蓝牙是不可用的", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		// 判断蓝牙是否打开，，没打开则弹出蓝牙提示打开蓝牙对话框
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (mChatService == null) {
				LogUtils.getInstance().e(getClass(), "----进行蓝牙相关设置---");
				setupChat();
			}
		}
	}

	@Override
	public synchronized void onResume() {
		super.onResume();
		LogUtils.getInstance().e(getClass(), "----onResume()");
		if (mChatService != null) {
			if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
				mChatService.startChat();
			}
		}
	}

	private EditText edt;

	/**
	 * 聊天需要的一些设置
	 */
	private void setupChat() {
		mConversationArrayAdapter = new ArrayAdapter<String>(this,
				R.layout.message);
		mConversationView = (ListView) findViewById(R.id.in);
		mConversationView.setAdapter(mConversationArrayAdapter);
		mSendButton = (Button) findViewById(R.id.button_send);
		mClearButton = (Button) findViewById(R.id.button_clear);
		edt = (EditText) findViewById(R.id.edit_text_out);
		mSendButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				String message = edt.getText().toString();
				sendMessage(message);
				edt.setText("");
			}
		});
		mClearButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mConversationArrayAdapter.clear();
			}
		});
		// 初始化BluetoothChatService进行蓝牙连接
		mChatService = new BluetoothChatService(this, mHandler);
	}

	/**
	 * 发送消息
	 * 
	 * @param message
	 *            发送的内容
	 */
	private void sendMessage(String message) {
		if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}
		if (message.length() > 0) {
			byte[] send = message.getBytes();
			mChatService.write(send);
		}
	}

	// 此Handler处理BluetoothChatService传来的消息
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_STATE_CHANGE:
				LogUtils.getInstance().e(getClass(),
						"MESSAGE_STATE_CHANGE: " + msg.arg1);
				switch (msg.arg1) {
				case BluetoothChatService.STATE_CONNECTED:
					mTitle.setText(R.string.devoice_connected_to);
					mTitle.append(mConnectedDeviceName);
					mConversationArrayAdapter.clear();
					break;
				case BluetoothChatService.STATE_CONNECTING:
					mTitle.setText(R.string.devoice_connecting);
					break;
				case BluetoothChatService.STATE_LISTEN:
				case BluetoothChatService.STATE_NONE:
					mTitle.setText(R.string.devoice_not_connected);
					break;
				}
				break;
			case MESSAGE_WRITE:
				byte[] writeBuf = (byte[]) msg.obj;
				String writeMessage = new String(writeBuf);
				mConversationArrayAdapter.add("Me:  " + writeMessage);
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// 读取到的数据
				String readMessage = new String(readBuf, 0, msg.arg1);
				mConversationArrayAdapter.add(mConnectedDeviceName + ":  "
						+ readMessage);
				Log.e("lichao", "收到消息" + readMessage);
				break;
			case MESSAGE_DEVICE_NAME:
				// 保存连接设备的名字
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"连接到" + mConnectedDeviceName, Toast.LENGTH_SHORT)
						.show();
				break;
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}
	};

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		LogUtils.getInstance().e(getClass(), "onActivityResult " + resultCode);
		switch (requestCode) {
		case REQUEST_CONNECT_DEVICE:
			// 当DeviceListActivity返回与设备连接的消息
			if (resultCode == Activity.RESULT_OK) {
				// 连接设备的MAC地址
				String address = data.getExtras().getString(
						DeviceListActivity.EXTRA_DEVICE_ADDRESS);
				// 得到蓝牙对象
				BluetoothDevice device = mBluetoothAdapter
						.getRemoteDevice(address);
				// 开始连接设备
				mChatService.connect(device);
			}
			break;
		case REQUEST_ENABLE_BT:
			// 判断蓝牙是否启用
			if (resultCode == Activity.RESULT_OK) {
				// 建立连接
				setupChat();
			} else {
				LogUtils.getInstance().e(getClass(), "蓝牙未启用");
				Toast.makeText(this, R.string.bt_not_enabled_leaving,
						Toast.LENGTH_SHORT).show();
				finish();
			}
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.btn_connect:
			// 连接设备
			Intent serverIntent = new Intent(this, DeviceListActivity.class);
			startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
			break;
		case R.id.btn_discover:
			// 允许被发现设备
			ensureDiscoverable();
			break;

		default:
			break;
		}
	}

	/**
	 * 允许设备被搜索
	 */
	private void ensureDiscoverable() {
		LogUtils.getInstance().e(getClass(), "----允许被搜索");
		if (mBluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
			startActivity(discoverableIntent);
		}
	}

	@Override
	public synchronized void onPause() {
		super.onPause();
		LogUtils.getInstance().e(getClass(), "----onPause()");
	}

	@Override
	public void onStop() {
		super.onStop();
		LogUtils.getInstance().e(getClass(), "----onStop()");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		// 停止蓝牙
		if (mChatService != null)
			mChatService.stop();
		LogUtils.getInstance().e(getClass(), "----onDestroy()");
	}

}