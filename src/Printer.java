/*
    Copyright 2013-2014 appPlant UG

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */

package org.laneveraroja.printer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.os.Bundle;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import com.bixolon.printer.BixolonPrinter;


public class Printer extends CordovaPlugin {

	// IMPRESORAS
	static BixolonPrinter mBixolonPrinter;

	private boolean mIsConnected;

	private String mConnectedDeviceName = null;

	private ListView mListView;
	private ProgressBar mProgressBar;

	private   static CordovaWebView webView = null;
	protected static Context context = null;
	private static UsbManager mManager = null;

	public static final String TAG = "Bixolon";

	private static final String ACTION_REQUEST_PERMISSION = "requestPermission";
	public static final String USB_PERMISSION ="de.appplant.cordova.plugin.printer.USB_PERMISSION";

	@Override
	public void initialize (CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);

		Printer.webView = super.webView;
		Printer.context = super.cordova.getActivity().getApplicationContext();
	}

	private Context getApplicationContext(){
		return super.cordova.getActivity().getApplicationContext();
	}

	public void onDestroy(){
		super.onDestroy();		
		try {
			context.unregisterReceiver(mUsbReceiver);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}
	@Override
	public boolean execute (String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		if ("print".equals(action)) {
			mBixolonPrinter = new BixolonPrinter(context, mHandler, null);	
			mBixolonPrinter.findUsbPrinters();

			return true;
		}

		// Imprime un ticket de prueba
		if ("printTestTicket".equals(action)) {
			mBixolonPrinter.printSelfTest(true);

			return true;
		}

		// Returning false results in a "MethodNotFound" error.
		return false;
	}

	private void isServiceAvailable (CallbackContext ctx) {

	}

	private BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.v(TAG, "mUsbReceiver.onReceive(" + context + ", " + intent + ")");
			String action = intent.getAction();
			context = getApplicationContext();
			//mBixolonPrinter.printSelfTest(true);

			if(USB_PERMISSION.equals(action)){
				synchronized(this){
					UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

					if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)){
						if(device != null){
							mBixolonPrinter.connect();
						}

					}
				}
			}		
			if (UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action)) {
				mBixolonPrinter.connect();				
				Toast.makeText(context, "Found USB device", Toast.LENGTH_SHORT).show();
			} else if (UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) {
				mBixolonPrinter.disconnect();
				Toast.makeText(context, "USB device removed", Toast.LENGTH_SHORT).show();
			}			
		}
	};



	private final Handler mHandler = new Handler(new Handler.Callback() {

		@SuppressWarnings("unchecked")
		@Override
		public boolean handleMessage(Message msg) {
			Log.d(TAG, "mHandler.handleMessage(" + msg + ")");

			switch (msg.what) {
			case BixolonPrinter.MESSAGE_STATE_CHANGE:
				switch (msg.arg1) {
				case BixolonPrinter.STATE_CONNECTED:
					//mListView.setEnabled(true);
					mIsConnected = true;
					break;

				case BixolonPrinter.STATE_CONNECTING:
					break;

				case BixolonPrinter.STATE_NONE:
					//mListView.setEnabled(false);
					mIsConnected = false;
					//mProgressBar.setVisibility(View.INVISIBLE);
					break;
				}
				return true;

			case BixolonPrinter.MESSAGE_READ:
				Printer.this.dispatchMessage(msg);
				return true;

			case BixolonPrinter.MESSAGE_DEVICE_NAME:
				mConnectedDeviceName = msg.getData().getString(BixolonPrinter.KEY_STRING_DEVICE_NAME);
				Toast.makeText(getApplicationContext(), mConnectedDeviceName, Toast.LENGTH_LONG).show();
				return true;

			case BixolonPrinter.MESSAGE_TOAST:
				//mListView.setEnabled(false);
				Toast.makeText(getApplicationContext(), msg.getData().getString(BixolonPrinter.KEY_STRING_TOAST), Toast.LENGTH_SHORT).show();
				return true;


			case BixolonPrinter.MESSAGE_USB_DEVICE_SET:
				if (msg.obj == null) {
					Toast.makeText(getApplicationContext(), "No connected device", Toast.LENGTH_SHORT).show();
				} else {					
					final Set<UsbDevice> usbDevices = (Set<UsbDevice>) msg.obj;
					final String[] items = new String[usbDevices.size()];					
					cordova.getActivity().runOnUiThread(new Runnable(){
						public void run(){							
							//DialogManager.showUsbDialog(getApplicationContext(), (Set<UsbDevice>) msg.obj, mUsbReceiver);
							int index = 0;
							for (UsbDevice device : usbDevices) {
								items[index++] = "Device name: " + device.getDeviceName() + ", Product ID: " + device.getProductId() + ", Device ID: " + device.getDeviceId();
							}

							new AlertDialog.Builder(cordova.getActivity()).setTitle("Impresoras USB conectadas")
							.setItems(items, new DialogInterface.OnClickListener() {

								public void onClick(DialogInterface dialog, int which) {
									mManager = (UsbManager) cordova.getActivity().getSystemService(Context.USB_SERVICE);
									PendingIntent pendingIntent = PendingIntent.getBroadcast(cordova.getActivity(), 0, new Intent(USB_PERMISSION), 0);

									// listen for new devices
									IntentFilter filter = new IntentFilter();

									filter.addAction(USB_PERMISSION);
									filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
									filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);

									context.registerReceiver(mUsbReceiver, filter);
									mManager.requestPermission((UsbDevice) usbDevices.toArray()[which], pendingIntent);
									Printer.mBixolonPrinter.connect((UsbDevice) usbDevices.toArray()[which]);
								}
							}).show();
						}
					});
				}
				return true;
			}
			return false;
		}
	});	

	private void dispatchMessage(Message msg) {
		switch (msg.arg1) {
		case BixolonPrinter.PROCESS_GET_STATUS:
			if (msg.arg2 == BixolonPrinter.STATUS_NORMAL) {
				Toast.makeText(getApplicationContext(), "No error", Toast.LENGTH_SHORT).show();
			} else {
				StringBuffer buffer = new StringBuffer();
				if ((msg.arg2 & BixolonPrinter.STATUS_COVER_OPEN) == BixolonPrinter.STATUS_COVER_OPEN) {
					buffer.append("Cover is open.\n");
				}
				if ((msg.arg2 & BixolonPrinter.STATUS_PAPER_NOT_PRESENT) == BixolonPrinter.STATUS_PAPER_NOT_PRESENT) {
					buffer.append("Paper end sensor: paper not present.\n");
				}

				Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_SHORT).show();
			}
			break;

		case BixolonPrinter.PROCESS_GET_TPH_THEMISTOR_STATUS:
			if (msg.arg2 == BixolonPrinter.STATUS_TPH_OVER_HEATING) {
				Toast.makeText(getApplicationContext(), "The status of TPH thermistor is overheating.", Toast.LENGTH_SHORT).show();
			}
			break;

		case BixolonPrinter.PROCESS_GET_POWER_MODE:
			if (msg.arg2 == BixolonPrinter.STATUS_SMPS_MODE) {
				Toast.makeText(getApplicationContext(), "SMPS mode", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "Battery mode", Toast.LENGTH_SHORT).show();
			}
			break;

		case BixolonPrinter.PROCESS_GET_BATTERY_VOLTAGE_STATUS:
			if (msg.arg2 == BixolonPrinter.STATUS_BATTERY_LOW_VOLTAGE) {
				Toast.makeText(getApplicationContext(), "Low voltage", Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "Normal voltage", Toast.LENGTH_SHORT).show();
			}
			break;

		case BixolonPrinter.PROCESS_GET_RECEIVE_BUFFER_DATA_SIZE:
			Toast.makeText(getApplicationContext(), "Size of data on receive buffer: " + msg.arg2 + " bytes", Toast.LENGTH_SHORT).show();
			break;

		case BixolonPrinter.PROCESS_GET_PRINTER_ID:
			Bundle data = msg.getData();
			Toast.makeText(getApplicationContext(), data.getString(BixolonPrinter.KEY_STRING_PRINTER_ID), Toast.LENGTH_SHORT).show();
			break;

		case BixolonPrinter.PROCESS_GET_BS_CODE_PAGE:
			data = msg.getData();
			Toast.makeText(getApplicationContext(), data.getString(BixolonPrinter.KEY_STRING_CODE_PAGE), Toast.LENGTH_SHORT).show();
			break;

		case BixolonPrinter.PROCESS_GET_PRINT_SPEED:
			switch (msg.arg2) {
			case BixolonPrinter.PRINT_SPEED_LOW:
				Toast.makeText(getApplicationContext(), "Print speed: low", Toast.LENGTH_SHORT).show();
				break;
			case BixolonPrinter.PRINT_SPEED_MEDIUM:
				Toast.makeText(getApplicationContext(), "Print speed: medium", Toast.LENGTH_SHORT).show();
				break;
			case BixolonPrinter.PRINT_SPEED_HIGH:
				Toast.makeText(getApplicationContext(), "Print speed: high", Toast.LENGTH_SHORT).show();
				break;
			}
			break;

		case BixolonPrinter.PROCESS_GET_PRINT_DENSITY:
			switch (msg.arg2) {
			case BixolonPrinter.PRINT_DENSITY_LIGHT:
				Toast.makeText(getApplicationContext(), "Print density: light", Toast.LENGTH_SHORT).show();
				break;
			case BixolonPrinter.PRINT_DENSITY_DEFAULT:
				Toast.makeText(getApplicationContext(), "Print density: default", Toast.LENGTH_SHORT).show();
				break;
			case BixolonPrinter.PRINT_DENSITY_DARK:
				Toast.makeText(getApplicationContext(), "Print density: dark", Toast.LENGTH_SHORT).show();
				break;
			}
			break;

		case BixolonPrinter.PROCESS_GET_POWER_SAVING_MODE:
			String text = "Power saving mode: ";
			if (msg.arg2 == 0) {
				text += false;
			} else {
				text += true + "\n(Power saving time: " + msg.arg2 + ")";
			}
			Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
			break;

		case BixolonPrinter.PROCESS_AUTO_STATUS_BACK:
			StringBuffer buffer = new StringBuffer(0);
			if ((msg.arg2 & BixolonPrinter.AUTO_STATUS_COVER_OPEN) == BixolonPrinter.AUTO_STATUS_COVER_OPEN) {
				buffer.append("Cover is open.\n");
			}
			if ((msg.arg2 & BixolonPrinter.AUTO_STATUS_NO_PAPER) == BixolonPrinter.AUTO_STATUS_NO_PAPER) {
				buffer.append("Paper end sensor: no paper present.\n");
			}

			if (buffer.capacity() > 0) {
				Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(getApplicationContext(), "No error.", Toast.LENGTH_SHORT).show();
			}
			break;

		case BixolonPrinter.PROCESS_EXECUTE_DIRECT_IO:
			buffer = new StringBuffer();
			data = msg.getData();
			byte[] response = data.getByteArray(BixolonPrinter.KEY_STRING_DIRECT_IO);
			for (int i = 0; i < response.length && response[i] != 0; i++) {
				buffer.append(Integer.toHexString(response[i]) + " ");
			}

			Toast.makeText(getApplicationContext(), buffer.toString(), Toast.LENGTH_SHORT).show();
			break;

		}
	}




























	/**
	 * Erstellt den Print-View.
	 */
	private Intent getPrintController (String appId) {
		String intentId = "android.intent.action.SEND";

		if (appId.equals("com.rcreations.send2printer")) {
			intentId = "com.rcreations.send2printer.print";
		} else if (appId.equals("com.dynamixsoftware.printershare")) {
			intentId = "android.intent.action.VIEW";
		} else if (appId.equals("com.hp.android.print")) {
			intentId = "org.androidprinting.intent.action.PRINT";
		}

		Intent intent = new Intent(intentId);

		if (appId != null)
			intent.setPackage(appId);

		return intent;
	}

	/**
	 * Stellt die Eigenschaften des Druckers ein.
	 */
	private void adjustSettingsForPrintController (Intent intent) {
		String mimeType = "image/png";
		String appId    = intent.getPackage();

		// Check for special cases that can receive HTML
		if (appId.equals("com.rcreations.send2printer") || appId.equals("com.dynamixsoftware.printershare")) {
			mimeType = "text/html";
		}

		intent.setType(mimeType);
	}

	/**
	 * Lädt den zu druckenden Content in ein WebView, welcher vom Drucker ausgedruckt werden soll.
	 */
	private void loadContentIntoPrintController (String content, Intent intent) {
		String mimeType = intent.getType();

		if (mimeType.equals("text/html")) {
			loadContentAsHtmlIntoPrintController(content, intent);
		} else {
			loadContentAsBitmapIntoPrintController(content, intent);
		}
	}

	/**
	 * Lädt den zu druckenden Content als HTML in ein WebView, welcher vom Drucker ausgedruckt werden soll.
	 */
	private void loadContentAsHtmlIntoPrintController (String content, Intent intent) {
		intent.putExtra(Intent.EXTRA_TEXT, content);
	}

	/**
	 * Lädt den zu druckenden Content als BMP in ein WebView, welcher vom Drucker ausgedruckt werden soll.
	 */
	private void loadContentAsBitmapIntoPrintController (String content, final Intent intent) {
		Activity ctx = cordova.getActivity();
		final WebView page = new WebView(ctx);
		final Printer self = this;

		page.setVisibility(View.INVISIBLE);
		page.getSettings().setJavaScriptEnabled(false);

		page.setWebViewClient( new WebViewClient() {
			@Override
			public void onPageFinished(final WebView page, String url) {
				new Handler().postDelayed( new Runnable() {
					@Override
					public void run() {
						Bitmap screenshot = self.takeScreenshot(page);
						File tmpFile      = self.saveScreenshotToTmpFile(screenshot);
						ViewGroup vg      = (ViewGroup)(page.getParent());

						intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));

						vg.removeView(page);
					}
				}, 1000);
			}
		});

		//Set base URI to the assets/www folder
		String baseURL = webView.getUrl();
		baseURL = baseURL.substring(0, baseURL.lastIndexOf('/') + 1);

		ctx.addContentView(page, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		page.loadDataWithBaseURL(baseURL, content, "text/html", "UTF-8", null);
	}

	/**
	 * Nimmt einen Screenshot der Seite auf.
	 */
	@SuppressWarnings("deprecation")
	private Bitmap takeScreenshot (WebView page) {
		Picture picture = page.capturePicture();
		Bitmap bitmap   = Bitmap.createBitmap(picture.getWidth(), picture.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas canvas   = new Canvas(bitmap);

		picture.draw(canvas);

		return bitmap;
	}

	/**
	 * Speichert den Screenshot der Seite in einer tmp. Datei ab.
	 */
	private File saveScreenshotToTmpFile (Bitmap screenshot) {
		try {
			File tmpFile = File.createTempFile("screenshot", ".tmp");
			FileOutputStream stream = new FileOutputStream(tmpFile);

			screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream);
			stream.close();

			return tmpFile;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Öffnet die Printer App, damit der Content ausgedruckt werden kann.
	 */
	private void startPrinterApp (Intent intent) {
		cordova.startActivityForResult(this, intent, 0);
	}

	/**
	 * Findet heraus, ob die Anwendung installiert ist.
	 */
	private boolean isAppInstalled (String appId) {
		PackageManager pm = cordova.getActivity().getPackageManager();

		try {
			PackageInfo pi = pm.getPackageInfo(appId, 0);

			if (pi != null){
				return true;
			}
		} catch (PackageManager.NameNotFoundException e) {}

		return false;
	}
}
