package com.hyadesinc.curiosus.lib;

import org.json.JSONException;
import org.json.JSONObject;

public class WirelessPasswordMessage implements IMessageBody {

	public String essid;
	public String password;
	public String key_mgmt;
    public Integer priority;

	public double lat;
	public double lon;


	public WirelessPasswordMessage(String essid, String password, String key_mgmt, Integer priority){
		this.essid = essid;
		this.password = password;
        this.key_mgmt = key_mgmt;
        this.priority = priority;
	}
	
	public WirelessPasswordMessage addLocation(double lat, double lon){
		this.lat = lat;
		this.lon = lon;
		return this;
	}




	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {

			obj.put("priority", priority);
			obj.put("key_mgmt", key_mgmt);
			obj.put("essid", essid);
			obj.put("password", password);
			
			if (lat != 0 && lon != 0) {
				obj.put("lat", lat);
				obj.put("lon", lon);
			}
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}

	
}
