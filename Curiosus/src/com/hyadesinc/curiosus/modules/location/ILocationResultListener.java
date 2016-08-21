package com.hyadesinc.curiosus.modules.location;

import com.hyadesinc.curiosus.lib.GPS;

import android.location.Location;

public interface ILocationResultListener {
	public void onLocationResult(Location location);
	public void onGsmLocationResult(GPS gps);
	public void onNoResult();
}
