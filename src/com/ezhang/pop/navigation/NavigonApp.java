package com.ezhang.pop.navigation;

import android.app.Activity;
import android.content.Intent;

class NavigonApp extends NavigationApp {
	Intent intent;

	public NavigonApp(Intent _intent) {
		super("Navigon");
		intent = _intent;
	}

	public void CallNavigateApp(Activity activity, String destLatitudue, String destLongitude) {
		String INTENT_EXTRA_KEY_LATITUDE = "latitude";
		String INTENT_EXTRA_KEY_LONGITUDE = "longitude";

		intent.putExtra(INTENT_EXTRA_KEY_LATITUDE,
				Float.parseFloat(destLatitudue));
		intent.putExtra(INTENT_EXTRA_KEY_LONGITUDE,
				Float.parseFloat(destLongitude));
		activity.startActivity(intent);
	}
}