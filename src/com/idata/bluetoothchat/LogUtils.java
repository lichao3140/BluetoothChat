package com.idata.bluetoothchat;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import android.text.TextUtils;
import android.util.Log;

public class LogUtils {

	/**
	 * ��־�ܿ���
	 */
	public static boolean SHOW_LOG_FLAG = true;
	/**
	 * DEBUG��־����
	 */
	private static boolean DEBUG = true;
	/**
	 * INFO��־����
	 */
	private static boolean INFO = true;
	/**
	 * WARN��־����
	 */
	private static boolean WARN = true;
	/**
	 * ERROR��־����
	 */
	private static boolean ERROR = true;

	private LogUtils() {
	}

	private static LogUtils logUtils = null;

	/**
	 * 
	 * @Title: getEcgCtroller
	 * @Description: ��ȡEcgCtrollerΨһʵ��
	 * @param @param ������Activity
	 * @param @param �û�Ψһid
	 * @param @return ��������
	 * @return EcgCtroller ��������
	 * @throws
	 */
	public static LogUtils getInstance() {
		if (logUtils == null) {
			logUtils = new LogUtils();
		}
		return logUtils;
	}

	public void d(Class<?> clazz, String logInfo) {
		print(Log.DEBUG, clazz.getSimpleName(), logInfo);
	}

	public void i(Class<?> clazz, String logInfo) {
		print(Log.INFO, clazz.getSimpleName(), logInfo);
	}

	public void w(Class<?> clazz, String logInfo) {
		print(Log.WARN, clazz.getSimpleName(), logInfo);
	}

	public void e(Class<?> clazz, String logInfo) {
		print(Log.ERROR, clazz.getSimpleName(), logInfo);
	}

	/**
	 * �������ֲ�ͬ�ӿ����� ��ӡ�������
	 * 
	 * @param index
	 * @param str
	 */
	private void print(int index, String mClassName, String logInfo) {
		if (!SHOW_LOG_FLAG) {
			return;
		}

		if (!TextUtils.isEmpty(logInfo)) {
			try {
				logInfo = logInfo.replaceAll("%(?![0-9a-fA-F]{2})", "%25");
				logInfo = URLDecoder.decode(logInfo, "utf-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			logInfo = "����ֵΪ��...";
		}
		String name = getFunctionName();
		if (name != null) {
			logInfo = name + " - " + logInfo;
		}

		// Close the debug log When DEBUG is false
		if (!DEBUG) {
			if (index <= Log.DEBUG) {
				return;
			}
		}
		// Close the info log When INFO is false
		if (!INFO) {
			if (index <= Log.INFO) {
				return;
			}
		}
		// Close the warn log When WARN is false
		if (!WARN) {
			if (index <= Log.WARN) {
				return;
			}
		}
		// Close the error log When ERROR is false
		if (!ERROR) {
			if (index <= Log.ERROR) {
				return;
			}
		}
		switch (index) {
		case Log.VERBOSE:
			Log.v(mClassName, logInfo.toString());
			break;
		case Log.DEBUG:
			Log.d(mClassName, logInfo.toString());
			break;
		case Log.INFO:
			Log.i(mClassName, logInfo.toString());
			break;
		case Log.WARN:
			Log.w(mClassName, logInfo.toString());
			break;
		case Log.ERROR:
			Log.e(mClassName, logInfo.toString());
			break;
		default:
			break;
		}
	}

	/**
	 * Get The Current Function Name
	 * 
	 * @return Name
	 */
	private String getFunctionName() {
		StackTraceElement[] sts = Thread.currentThread().getStackTrace();
		if (sts == null) {
			return null;
		}
		for (StackTraceElement st : sts) {
			if (st.isNativeMethod()) {
				continue;
			}
			if (st.getClassName().equals(Thread.class.getName())) {
				continue;
			}
			if (st.getClassName().equals(this.getClass().getName())) {
				continue;
			}
			return "[ " + Thread.currentThread().getName() + ": "
					+ st.getFileName() + ":" + st.getLineNumber() + " "
					+ st.getMethodName() + " ]";
		}
		return null;
	}

}
