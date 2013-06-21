package com.ezhang.pop.core;

import android.location.Criteria;
import android.location.LocationManager;

public class LocationService {

	public static String GetBestProvider(LocationManager locationManager) {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);

		return locationManager.getBestProvider(criteria, true);
	}

}
