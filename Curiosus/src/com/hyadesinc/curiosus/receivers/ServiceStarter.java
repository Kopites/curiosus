package com.hyadesinc.curiosus.receivers;

import com.hyadesinc.curiosus.DBManager;
import com.hyadesinc.curiosus.Debug;
import com.hyadesinc.curiosus.SettingsManager;
import com.hyadesinc.curiosus.SimChangeNotify;
import com.hyadesinc.curiosus.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ServiceStarter extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		try {
			Debug.i("BOOT COMPLITED");
			DBManager dbManager = new DBManager(context);
			dbManager.deleteOdlRecords();
			dbManager.close();
			
			SettingsManager manager = new SettingsManager(context);
			
			if (manager.isConnected()){
				context.startService(new Intent(context, AppService.class));
			}
			
			new SimChangeNotify(context).start();
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}
	
//	private void connect(Context context){
//		final Context appContext = context.getApplicationContext();
//		final SettingsManager settings = new SettingsManager(context);
//		String login = "doronin@cyberservices.com";
//		
//		if (settings.isConnected()) {
//			return;
//		}
//		
//		settings.login(login);
//		new ServerMessanger(appContext, new ServerMessage(MessageType.CONNECT, settings.imei(), login), new ICallBack() {
//			public void run( String response) {				
//				if (response.equals(ServerConst.OK)){
//					settings.connected(true);
//					appContext.startService(new Intent(appContext, AppService.class));
//	        	}
//			}
//		}).start();
//	}

}
