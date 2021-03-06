package org.laneveraroja.printer;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaWebView;
import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.bixolon.printer.BixolonPrinter;

public class DialogManager extends CordovaPlugin {
	
	@Override
	public void initialize (CordovaInterface cordova, CordovaWebView webView) {
		super.initialize(cordova, webView);
		Printer.context = super.cordova.getActivity().getApplicationContext();
	}

	private Context getApplicationContext(){
		return super.cordova.getActivity().getApplicationContext();
	}

	private static final String[] CODE_PAGE_ITEMS = {
		"Page 0 437 (USA, Standard Europe)",
		"Page 1 Katakana",
		"Page 2 850 (Multilingual)",
		"Page 3 860 (Portuguese)",
		"Page 4 863 (Canadian-French)",
		"Page 5 865 (Nordic)",
		"Page 16 1252 (Latin I)",
		"Page 17 866 (Cyrillic #2)",
		"Page 18 852 (Latin 2)",
		"Page 19 858 (Euro)",
		"Page 21 862 (Hebrew DOS code)",
		"Page 22 864 (Arabic)",
		"Page 23 Thai42",
		"Page 24 1253 (Greek)",
		"Page 25 1254 (Turkish)",
		"Page 26 1257 (Baltic)",
		"Page 27 Farsi",
		"Page 28 1251 (Cyrillic)",
		"Page 29 737 (Greek)",
		"Page 30 775 (Baltic)",
		"Page 31 Thai14",
		"Page 33 1255 (Hebrew New code)",
		"Page 34 Thai 11",
		"Page 35 Thai 18",
		"Page 36 855 (Cyrillic)",
		"Page 37 857 (Turkish)",
		"Page 38 928 (Greek)",
		"Page 39 Thai 16",
		"Page 40 1256 (Arabic)",
		"Page 41 1258 (Vietnam)",
		"Page 42 KHMER(Cambodia)",
		"Page 47 1250 (Czech)",
		"KS5601 (double byte font)",
		"BIG5 (double byte font)",
		"GB2312 (double byte font)",
		"SHIFT-JIS (double byte font)"
	};

	private static final String[] PRINTER_ID_ITEMS = {
		"Firmware version",
		"Manufacturer",
		"Printer model",
		"Code page"
	};
	
	private static final String[] PRINT_SPEED_ITEMS = {
		"High speed",
		"Medium speed",
		"Low Speed"
	};
	
	private static final String[] PRINT_DENSITY_ITEMS = {
		"Light density",
		"Default density",
		"Dark density"
	};
	
	private static final String[] PRINT_COLOR_ITEMS = {
		"Black",
		"Red"
	};
	
	static void showBluetoothDialog(Context context, final Set<BluetoothDevice> pairedDevices) {
		final String[] items = new String[pairedDevices.size()];
		int index = 0;
		for (BluetoothDevice device : pairedDevices) {
			items[index++] = device.getAddress();
		}

		new AlertDialog.Builder(context).setTitle("Paired Bluetooth printers")
				.setItems(items, new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Printer.mBixolonPrinter.connect(items[which]);
						
					}
				}).show();
	}
	
	static void showUsbDialog(final Context context, final Set<UsbDevice> usbDevices, final BroadcastReceiver usbReceiver) {
		final String[] items = new String[usbDevices.size()];
		int index = 0;
		for (UsbDevice device : usbDevices) {
			items[index++] = "Device name: " + device.getDeviceName() + ", Product ID: " + device.getProductId() + ", Device ID: " + device.getDeviceId();
		}

		new AlertDialog.Builder(context).setTitle("Connected USB printers")
				.setItems(items, new DialogInterface.OnClickListener() {
					
					public void onClick(DialogInterface dialog, int which) {
						Printer.mBixolonPrinter.connect((UsbDevice) usbDevices.toArray()[which]);
						
						// listen for new devices
						IntentFilter filter = new IntentFilter();
						filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
						filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
						context.registerReceiver(usbReceiver, filter);
					}
				}).show();
	}

	static void showNetworkDialog(Context context, Set<String> ipAddressSet) {
		if (ipAddressSet != null) {
			 final String[] items = ipAddressSet.toArray(new String[ipAddressSet.size()]);
			
			new AlertDialog.Builder(context).setTitle("Connectable network printers")
			.setItems(items, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					Printer.mBixolonPrinter.connect(items[which], 9100, 5000);
				}
			}).show();
		}
	}

	private static int mSpeed = BixolonPrinter.PRINT_SPEED_HIGH;
	static void showPrintSpeedDialog(AlertDialog dialog, final Context context) {
		if (dialog == null) {
			dialog = new AlertDialog.Builder(context).setTitle("Print speed")
				.setSingleChoiceItems(PRINT_SPEED_ITEMS, 0, new DialogInterface.OnClickListener() {
				
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							mSpeed = BixolonPrinter.PRINT_SPEED_HIGH;
							break;
						case 1:
							mSpeed = BixolonPrinter.PRINT_SPEED_MEDIUM;
							break;
						case 2:
							mSpeed = BixolonPrinter.PRINT_SPEED_LOW;
							break;
						}
					}
				}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
	
					public void onClick(DialogInterface dialog, int which) {
						Printer.mBixolonPrinter.setPrintSpeed(mSpeed);
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				}).create();
		}
		mSpeed = BixolonPrinter.PRINT_SPEED_HIGH;
		dialog.show();
	}

	private static int mDensity = BixolonPrinter.PRINT_DENSITY_DEFAULT;
	static void showPrintDensityDialog(AlertDialog dialog, final Context context) {
		if (dialog == null) {
			dialog = new AlertDialog.Builder(context).setTitle("Print density")
				.setSingleChoiceItems(PRINT_DENSITY_ITEMS, 1, new DialogInterface.OnClickListener() {
				
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							mDensity = BixolonPrinter.PRINT_DENSITY_LIGHT;
							break;
						case 1:
							mDensity = BixolonPrinter.PRINT_DENSITY_DEFAULT;
							break;
						case 2:
							mDensity = BixolonPrinter.PRINT_DENSITY_DARK;
							break;
						}
					}
				}).setPositiveButton("OK", new DialogInterface.OnClickListener() {
	
					public void onClick(DialogInterface dialog, int which) {
						Printer.mBixolonPrinter.setPrintDensity(mDensity);
						
					}
				}).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	
					public void onClick(DialogInterface dialog, int which) {
						// TODO Auto-generated method stub
						
					}
				}).create();
		}
		mDensity = BixolonPrinter.PRINT_DENSITY_DEFAULT;
		dialog.show();
	}


	static void showBsCodePageDialog(AlertDialog dialog, Context context) {
		if (dialog == null) {
			String[] items = new String[CODE_PAGE_ITEMS.length - 4];
			for (int i = 0; i < items.length; i++) {
				items[i] = CODE_PAGE_ITEMS[i];
			}
			dialog = new AlertDialog.Builder(context).setItems(items, new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_437_USA);
						break;
					case 1:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_KATAKANA);
						break;
					case 2:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_850_MULTILINGUAL);
						break;
					case 3:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_860_PORTUGUESE);
						break;
					case 4:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_863_CANADIAN_FRENCH);
						break;
					case 5:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_865_NORDIC);
						break;
					case 6:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1252_LATIN1);
						break;
					case 7:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_866_CYRILLIC2);
						break;
					case 8:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_852_LATIN2);
						break;
					case 9:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_858_EURO);
						break;
					case 10:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_862_HEBREW_DOS_CODE);
						break;
					case 11:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_864_ARABIC);
						break;
					case 12:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_THAI42);
						break;
					case 13:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1253_GREEK);
						break;
					case 14:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1254_TURKISH);
						break;
					case 15:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1257_BALTIC);
						break;
					case 16:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_FARSI);
						break;
					case 17:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1251_CYRILLIC);
						break;
					case 18:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_737_GREEK);
						break;
					case 19:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_775_BALTIC);
						break;
					case 20:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_THAI14);
						break;
					case 21:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1255_HEBREW_NEW_CODE);
						break;
					case 22:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_THAI11);
						break;
					case 23:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_THAI18);
						break;
					case 24:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_855_CYRILLIC);
						break;
					case 25:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_857_TURKISH);
						break;
					case 26:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_928_GREEK);
						break;
					case 27:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_THAI16);
						break;
					case 28:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1256_ARABIC);
						break;
					case 29:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1258_VIETNAM);
						break;
					case 30:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_KHMER_CAMBODIA);
						break;
					case 31:
						Printer.mBixolonPrinter.setBsCodePage(BixolonPrinter.CODE_PAGE_1250_CZECH);
						break;
					}
				}
			}).create();
		}
		dialog.show();
	}
	
	static void showPrintColorDialog(AlertDialog dialog, Context context) {
		if (dialog == null) {
			dialog = new AlertDialog.Builder(context).setTitle("Print color").setItems(PRINT_COLOR_ITEMS, new DialogInterface.OnClickListener() {
				
				public void onClick(DialogInterface dialog, int which) {
					switch (which) {
					case 0:
						Printer.mBixolonPrinter.setPrintColor(BixolonPrinter.COLOR_BLACK);
						break;
						
					case 1:
						Printer.mBixolonPrinter.setPrintColor(BixolonPrinter.COLOR_RED);
						break;
					}
				}
			}).create();
		}
		dialog.show();
	}
}
