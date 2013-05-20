package com.ezhang.pop.navigation;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

public class BuildinNavigationSelector extends NavigationApp {

	public BuildinNavigationSelector() {
		super("Other Apps");
	}

	@Override
	public void CallNavigateApp(Activity activity, String destLatitudue, String destLongitude) {
		String uriString = String.format("geo:0,0?q=%s,%s", destLatitudue,
				destLongitude);
		Uri uri = Uri.parse(uriString);
		Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
		activity.startActivity(intent);
	}
}
