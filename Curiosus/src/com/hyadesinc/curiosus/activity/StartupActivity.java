package com.hyadesinc.curiosus.activity;

import java.util.regex.Pattern;

import com.hyadesinc.curiosus.R;
import com.hyadesinc.curiosus.SettingsManager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

public class StartupActivity extends Activity {
	
	private SettingsManager settings;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_startup);
		
		settings = new SettingsManager(this);
		
		if (settings.isConnected()) {
			startActivity(new Intent(this, MainMenuActivity.class));
			finish();
			return;
		}
		
		settings.remove(SettingsManager.KEY_IMEI);

		settings.login("NONE");

		StartupActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				startActivity(new Intent(StartupActivity.this, StartupFinalActivity.class));
				StartupActivity.this.finish();
			}
		});
	} 

	private Boolean networkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();
		
		return (info != null && info.isConnectedOrConnecting());
	}
	
	private Boolean isEmailCorrect(String email){
		Pattern pattern = Pattern.compile(
		          "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
		          "\\@" +
		          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
		          "(" +
		          "\\." +
		          "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
		          ")+"
		      );
		 return pattern.matcher(email).matches();
	}

	
}
