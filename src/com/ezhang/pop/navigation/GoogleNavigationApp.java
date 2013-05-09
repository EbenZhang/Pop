package com.ezhang.pop.navigation;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

class GoogleNavigationApp extends NavigationApp {
	public GoogleNavigationApp() {
		super("Google Maps");
	}

	public void CallNavigateApp(Activity activity, String srcLatitude,
			String srcLongitude, String destLatitudue, String destLongitude) {
		String uriString = String.format("geo:0,0?q=%s,%s", destLatitudue,
				destLongitude);
		Uri uri = Uri.parse(uriString);
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
		activity.startActivity(intent);
		intent.setClassName("com.google.android.apps.maps",
				"com.google.android.maps.MapsActivity");
		activity.startActivity(intent);
	}
}