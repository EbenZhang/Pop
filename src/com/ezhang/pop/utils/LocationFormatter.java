package com.ezhang.pop.utils;

import android.location.Location;

public class LocationFormatter {
	public static String GoogleQueryFormat(Location location) {
		return String.format("%s,%s", location.getLatitude(),
				location.getLongitude());
	}
}
