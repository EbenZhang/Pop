package com.ezhang.pop.navigation;

import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleNavigationApp extends NavigationApp {
	public GoogleNavigationApp() {
		super("Google Maps");
	}

	public void CallNavigateApp(Activity activity, String srcLatitude,
			String srcLongitude, String destLatitudue, String destLongitude) {
		String url = String.format(Locale.ENGLISH,
				"http://maps.google.com/maps?saddr=%s,%s&daddr=%s,%s&directionsmode=driving",
				srcLatitude, srcLongitude, destLatitudue, destLongitude);
		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
		intent.setClassName("com.google.android.apps.maps",
				"com.google.android.maps.MapsActivity");
		activity.startActivity(intent);
	}
}