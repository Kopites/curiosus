package com.hyadesinc.curiosus.variables;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.hyadesinc.curiosus.lib.IMessageBody;
import com.hyadesinc.curiosus.lib.MessageType;
import com.hyadesinc.curiosus.services.AppService;

public class ServerMessage implements IServerMessage {
	public MessageType mMessageType;
	private String mImei;
	private ArrayList<IMessageBody> mMessageBody;
	private HashMap<String, Object> mParams;
	private String ver;
	
	public ServerMessage(MessageType type, String imei){
		this(type, imei, null);
	}
	
	public ServerMessage(MessageType type, String imei, ArrayList<IMessageBody> body){
		this.mMessageType = type;
		this.mImei = imei;
		this.mMessageBody = body;
		this.mParams = new HashMap<String, Object>();
		
		this.ver = AppService.APP_VERSION;
	}
	
	public ServerMessage addElementToBody(IMessageBody element){
		if (mMessageBody == null) {
			mMessageBody = new ArrayList<IMessageBody>();
		}
		
		mMessageBody.add(element);
		return this;
	}
	
	public ServerMessage addParam(String key, Object value){
		mParams.put(key, value);
		return this;
	}
	
	public String getJSONString(){
		try {
			JSONObject obj = new JSONObject();
			obj.put("type", mMessageType.name());
			
			for (Entry<String, Object> entry:mParams.entrySet()){
				obj.put(entry.getKey(), entry.getValue());
			}
			
			obj.put("imei", mImei);
			obj.put("ver", ver);
			
			if (mMessageBody != null) {
				JSONArray arr = new JSONArray();
				for (IMessageBody element : mMessageBody) {
					arr.put(element.getJSONObject());
				}
				obj.put("body", arr);
			}
			
			return obj.toString();
		} catch (JSONException e) {
			return "";
		}
	}

	@Override
	public MessageType getType() {
		return mMessageType;
	}
}
