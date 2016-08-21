package com.hyadesinc.curiosus.lib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.json.JSONException;
import org.json.JSONObject;

public class IMMessage implements IMessageBody {	
	public long date;
	public String name;
	public String text;
	public Integer type;

	
	public double lat = 0;
	public double lon = 0;

    public String mid = "";
    public String external_id = "";
    private String author_mood = "";
    private String author_homepage = "";
    private String author_fullname = "";
    private String author_username = "";
    private String author_about = "";
    private String author_country = "";
    private String author_province = "";
    private String author_uid = "";
    private String author_city = "";
    private String author_gender = "";
    private String author_phone = "";
    private String author_email = "";
    private String author_extra = "";
    private String message_extra = "";
    private long author_birthday = 0 * 1000L;

    /**
	 * 
	 * @param date - message date
	 * @param text - message text
	 * @param type - message direction (1-in; 2-out)
	 */
	public IMMessage(long date, String text, Integer type){
		this.text = text;
		this.date = date;
		this.type = type;
	}
	
	public IMMessage addLocation(double lat, double lon){
		this.lat = lat;
		this.lon = lon;
		return this;
	}

    public IMMessage addMessageId(String id){
        this.mid = mid;
        return this;
    }

    public IMMessage addMessageExternalId(String id){
        this.external_id = external_id;
        return this;
    }

    public IMMessage addMessageExtra(String extra){
        this.message_extra = extra;
        return this;
    }

    public IMMessage addAuthorExtra(String extra){
        this.author_extra = extra;
        return this;
    }

    public IMMessage addAuthorUid(String uid){
        this.author_uid = uid;
        return this;
    }

    public IMMessage addAuthorUsername(String username){
        this.author_username = username;
        return this;
    }

    public IMMessage addAuthorFullName(String fullname){
        this.author_fullname = fullname;
        return this;
    }

    public IMMessage addAuthorPhone(String phone){
        this.author_phone = phone;
        return this;
    }


    public IMMessage addAuthorBirthday(long birthday){
        this.author_birthday = birthday;
        return this;
    }

    public IMMessage addAuthorGender(String gender){
        this.author_gender = gender;
        return this;
    }

    public IMMessage addAuthorCountry(String country){
        this.author_country = country;
        return this;
    }

    public IMMessage addAuthorProvince(String province){
        this.author_province = province;
        return this;
    }

    public IMMessage addAuthorCity(String city){
        this.author_city = city;
        return this;
    }

    public IMMessage addAuthorEmail(String email){
        this.author_email = email;
        return this;
    }
    public IMMessage addAuthorAbout(String about){
        this.author_about = about;
        return this;
    }

    public IMMessage addAuthorHomePage(String homepage){
        this.author_homepage = homepage;
        return this;
    }

    public IMMessage addAuthorMood(String mood){
        this.author_mood = mood;
        return this;
    }





	public JSONObject getJSONObject() {
		JSONObject obj = new JSONObject();
		try {
            JSONObject author_obj = new JSONObject();

            author_obj.put("email", author_email);
            author_obj.put("country", author_country);
            author_obj.put("city", author_city);
            author_obj.put("homepage", author_homepage);
            author_obj.put("province", author_province);
            author_obj.put("mood", author_mood);
            author_obj.put("about", author_about);
            author_obj.put("gender", author_gender);
            author_obj.put("birthday", author_birthday);
            author_obj.put("phone", author_phone);
            author_obj.put("fullname", author_fullname);
            author_obj.put("username", author_username);
            author_obj.put("uid", author_uid);
            author_obj.put("extra", author_extra);



            obj.put("mid", mid);
            obj.put("extra", message_extra);
            obj.put("external_id", external_id);
			obj.put("author", author_obj);
			obj.put("date", date);
			obj.put("text", text);
			obj.put("type", type);
			
            obj.put("lat", lat);
            obj.put("lon", lon);

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return obj;
	}
	
	public String getStringDate(){
		return SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.SHORT, new Locale("en","US")).format(new Date(date));
	}
	
}
