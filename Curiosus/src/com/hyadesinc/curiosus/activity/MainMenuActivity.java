package com.hyadesinc.curiosus.activity;

import java.util.Calendar;

import org.acra.ACRA;

import com.hyadesinc.curiosus.R;
import com.hyadesinc.curiosus.ServerMessanger;
import com.hyadesinc.curiosus.SettingsManager;
import com.hyadesinc.curiosus.SimChangeNotify;
import com.hyadesinc.curiosus.lib.MessageType;
import com.hyadesinc.curiosus.services.AppService;
import com.hyadesinc.curiosus.variables.ServerMessage;
import com.stericson.RootTools.RootTools;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class MainMenuActivity extends Activity {	
	private SettingsManager settings;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mainmenu);
        
//        DBManager m = new DBManager(this);
//        m.getReadableDatabase().close();
//        m.close();
        
        settings = new SettingsManager(this);
        
        renderElements();
        
        new SimChangeNotify(this).start();
        
        if (settings.isConnected()){
        	startService(new Intent(this, AppService.class));
        }
        else{
        	stopService(new Intent(this, AppService.class));
        }
       
        if (RootTools.isRootAvailable()) {
			RootTools.isAccessGiven();
        }
        
        if (AppService.sThreadManager != null) {
			SettingsManager settings = new SettingsManager(this);
			AppService.sThreadManager.addTask(
				new ServerMessanger(
			        this,
			        new ServerMessage(MessageType.SETTINGS_SEND, settings.imei())
			        	.addParam("settings", settings.getJSON())
			        	.addParam("date", Calendar.getInstance().getTimeInMillis())
				)
			);	
		}
	}
	
	@Override
	protected void onResume() {
		renderElements();
		
		super.onResume();
	}
	
	private void renderElements(){
        TextView pnText = (TextView)findViewById(R.id.phoneNumberTextField);
        pnText.setText(settings.imei());
        
        TextView loginText = (TextView)findViewById(R.id.loginTextField);
        loginText.setText(settings.login());
        
        findViewById(R.id.settingsButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	        	startActivity(new Intent(MainMenuActivity.this, MyPreferenceActivity.class));
			}
		});
        
        findViewById(R.id.filterButton).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	        	startActivity(new Intent(MainMenuActivity.this, FilterActivity.class));
			}
		});
        
        try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			TextView versionView = (TextView)findViewById(R.id.mainmenu_version);
			if (info != null) {
				versionView.setText(info.versionName + " (" + info.versionCode + ")");
			}
			
		} catch (Exception e) {
			ACRA.getErrorReporter().handleSilentException(e);
		}

	}	
}
