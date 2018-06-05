package com.idata.bluetoothchat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * �������������������
 */
public class BluetoothChatService {
	// ��������
	private static final String NAME = "BluetoothChat";

	// ����һ��Ψһ��UUID
	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private final BluetoothAdapter mAdapter;
	private final Handler mHandler;
	private AcceptThread mAcceptThread;
	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;
	private int mState;

	// ����,��ʾ��ǰ������״̬
	public static final int STATE_NONE = 0;
	public static final int STATE_LISTEN = 1;
	public static final int STATE_CONNECTING = 2;
	public static final int STATE_CONNECTED = 3;

	public BluetoothChatService(Context context, Handler handler) {
		mAdapter = BluetoothAdapter.getDefaultAdapter();
		mState = STATE_NONE;
		mHandler = handler;
	}

	/**
	 * ���õ�ǰ������״̬
	 * 
	 * @param state
	 *            ����״̬
	 */
	private synchronized void setState(int state) {
		mState = state;
		// ֪ͨActivity����UI
		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE,
				state, -1).sendToTarget();
	}

	/**
	 * ���ص�ǰ����״̬
	 * 
	 */
	public synchronized int getState() {
		return mState;
	}

	/**
	 * ��ʼ�������
	 * 
	 */
	public void startChat() {
		LogUtils.getInstance().e(getClass(), "start ");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread == null) {
			mAcceptThread = new AcceptThread();
			mAcceptThread.start();
		}
		setState(STATE_LISTEN);
	}

	/**
	 * ����Զ���豸
	 * 
	 * @param device
	 *            ����
	 */
	public synchronized void connect(BluetoothDevice device) {

		if (mState == STATE_CONNECTING) {
			if (mConnectThread != null) {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		mConnectThread = new ConnectThread(device);
		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	/**
	 * ����ConnectedThread��ʼ����һ����������
	 */
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		LogUtils.getInstance().e(getClass(), "���� ");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}

		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();
		// ���ӳɹ���֪ͨactivity
		Message msg = mHandler
				.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);
		setState(STATE_CONNECTED);
	}

	/**
	 * ֹͣ�����߳�
	 */
	public synchronized void stop() {
		LogUtils.getInstance().e(getClass(), "---stop()");
		setState(STATE_NONE);
		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
		if (mAcceptThread != null) {
			mAcceptThread.cancel();
			mAcceptThread = null;
		}
	}

	/**
	 * �Է�ͬ����ʽд��ConnectedThread
	 * 
	 * @param out
	 */
	public void write(byte[] out) {
		ConnectedThread r;
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		}
		r.write(out);
	}

	/**
	 * �޷����ӣ�֪ͨActivity
	 */
	private void connectionFailed() {
		setState(STATE_LISTEN);
		Message msg = mHandler
				.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "�޷������豸");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * �豸�Ͽ����ӣ�֪ͨActivity
	 */
	private void connectionLost() {
		Message msg = mHandler
				.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "�豸�Ͽ�����");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	/**
	 * �������������
	 */
	private class AcceptThread extends Thread {
		private final BluetoothServerSocket mmServerSocket;

		public AcceptThread() {
			BluetoothServerSocket tmp = null;

			try {
				tmp = mAdapter
						.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(), "--��ȡsocketʧ��:" + e);
			}
			mmServerSocket = tmp;
		}

		public void run() {
			setName("AcceptThread");
			BluetoothSocket socket = null;
			while (mState != STATE_CONNECTED) {
				LogUtils.getInstance().e(getClass(), "----accept-ѭ��ִ����-");
				try {
					socket = mmServerSocket.accept();
				} catch (IOException e) {
					LogUtils.getInstance().e(getClass(), "accept() ʧ��" + e);
					break;
				}

				// ������ӱ�����
				if (socket != null) {
					synchronized (BluetoothChatService.this) {
						switch (mState) {
						case STATE_LISTEN:
						case STATE_CONNECTING:
							// ��ʼ�����߳�
							connected(socket, socket.getRemoteDevice());
							break;
						case STATE_NONE:
						case STATE_CONNECTED:
							// û��׼���û��Ѿ�����
							try {
								socket.close();
							} catch (IOException e) {
								LogUtils.getInstance().e(getClass(),
										"���ܹر���Щ����" + e);
							}
							break;
						}
					}
				}
			}
			LogUtils.getInstance().e(getClass(), "����mAcceptThread");
		}

		public void cancel() {
			LogUtils.getInstance().e(getClass(), "ȡ�� " + this);
			try {
				mmServerSocket.close();
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(), "�ر�ʧ��" + e);
			}
		}
	}

	/**
	 * @description:���������߳�
	 * @author��zzq
	 * @time: 2016-8-6 ����1:18:41
	 */
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(), "socket��ȡʧ�ܣ�" + e);
			}
			mmSocket = tmp;
		}

		public void run() {
			LogUtils.getInstance().e(getClass(), "��ʼmConnectThread");
			setName("ConnectThread");
			// mAdapter.cancelDiscovery();
			try {
				mmSocket.connect();
			} catch (IOException e) {
				// ����ʧ�ܣ�����ui
				connectionFailed();
				try {
					mmSocket.close();
				} catch (IOException e2) {
					LogUtils.getInstance().e(getClass(), "�ر�����ʧ��" + e2);
				}
				// ������������߳�
				startChat();
				return;
			}

			synchronized (BluetoothChatService.this) {
				mConnectThread = null;
			}

			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(), "�ر�����ʧ��" + e);
			}
		}
	}

	/**
	 * �Ѿ����ӳɹ�����߳� �������д���ʹ����Ĵ���
	 */
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;
			// �õ�BluetoothSocket����������
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(),
						"temp sockets not created" + e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}
		public void run() {
			int bytes;
			String str1 = "";
			// ѭ��������Ϣ
			while (true) {
				try {
					byte[] buffer = new byte[256];

					bytes = mmInStream.read(buffer);
//					buffer2.AddToHistory(buffer, bytes);
					String readStr = new String(buffer, 0, bytes);// �ֽ�����ֱ��ת�����ַ���
					String str = bytes2HexString(buffer).replaceAll("00", "")
							.trim();
					if (bytes > 0) {// ����ȡ������Ϣ�������߳�
						mHandler.obtainMessage(
								MainActivity.MESSAGE_READ, bytes, -1,
								buffer).sendToTarget();

					} else {
						LogUtils.getInstance().e(getClass(), "disconnected");
						connectionLost();

						if (mState != STATE_NONE) {
							LogUtils.getInstance()
									.e(getClass(), "disconnected");
							startChat();
						}
						break;
					}
				} catch (IOException e) {
					LogUtils.getInstance().e(getClass(), "disconnected" + e);
					connectionLost();

					if (mState != STATE_NONE) {
						// ��������������ģʽ�����÷���
						startChat();
					}
					break;
				}
			}
		}

		/**
		 * д��OutStream����
		 * 
		 * @param buffer
		 *            Ҫд���ֽ�
		 */
		public void write(byte[] buffer) {
			try {
				mmOutStream.write(buffer);

				// ����Ϣ����UI
				mHandler.obtainMessage(MainActivity.MESSAGE_WRITE, -1,
						-1, buffer).sendToTarget();
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(),
						"Exception during write:" + e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				LogUtils.getInstance().e(getClass(),
						"close() of connect socket failed:" + e);
			}
		}
	}

	/**
	 * ���ֽ����鵽ʮ�������ַ���ת��
	 */
	public static String bytes2HexString(byte[] b) {
		String ret = "";
		for (int i = 0; i < b.length; i++) {
			String hex = Integer.toHexString(b[i] & 0xFF);
			if (hex.length() == 1) {
				hex = '0' + hex;
			}
			ret += hex.toUpperCase();
		}
		return ret;
	}

	// ���Դ���
	// if (str.endsWith("0D")) {
	// byte[] buffer1 = (str1 + readStr).getBytes();
	// mHandler.obtainMessage(
	// BluetoothChatActivity.MESSAGE_READ,
	// buffer1.length, -1, buffer1).sendToTarget();
	// str1 = "";
	// Log.i("ttt", "------���1");
	// } else {
	// if (!str.contains("0A")) {
	// str1 = str1 + readStr;
	// Log.i("ttt", "------���2");
	// } else {
	// if (!str.equals("0A") && str.endsWith("0A")) {
	// Log.i("ttt", "------���3");
	// mHandler.obtainMessage(
	// BluetoothChatActivity.MESSAGE_READ,
	// bytes, -1, buffer).sendToTarget();
	// }
	// }
	// }
}
