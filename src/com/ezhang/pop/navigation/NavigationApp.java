package com.ezhang.pop.navigation;

import android.app.Activity;

public abstract class NavigationApp {
	public String desc;

	public NavigationApp(String _desc) {
		desc = _desc;
	}

	public abstract void CallNavigateApp(Activity activity,
			String srcLatitude, String srcLongitude, String destLatitudue,
			String destLongitude);
}