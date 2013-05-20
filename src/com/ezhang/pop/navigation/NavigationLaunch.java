package com.ezhang.pop.navigation;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;

public class NavigationLaunch implements OnClickListener {
	List<NavigationApp> m_apps;
	Activity m_activity;
	String m_dstLatitude;
	String m_dstLongitude;

	public NavigationLaunch(Activity activity, String dstLatitude, String dstLongitude) {
		m_activity = activity;
		m_apps = GetNavigationApps(activity);
		m_dstLatitude = dstLatitude;
		m_dstLongitude = dstLongitude;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		this.m_apps.get(which).CallNavigateApp(this.m_activity, m_dstLatitude, m_dstLongitude);
	}

	public void Launch() {
		if (m_apps.size() == 1) {
			onClick(null, 0);
			return;
		}
		String[] appsDesc = new String[m_apps.size()];
		int i = 0;
		for (NavigationApp app : this.m_apps) {
			appsDesc[i++] = app.desc;
		}
		AlertDialog dialog = new AlertDialog.Builder(this.m_activity)
				.setTitle("Select Navigation App").setItems(appsDesc, this)
				.create();
		dialog.show();
	}

	private List<NavigationApp> GetNavigationApps(Activity activity) {
		List<NavigationApp> apps = new ArrayList<NavigationApp>();
		PackageManager m_pm = activity.getPackageManager();

		Intent gmaps = m_pm
				.getLaunchIntentForPackage("com.google.android.apps.maps");
		if (gmaps != null) {
			apps.add(new GoogleNavigationApp());
		}

		Intent navigon = m_pm
				.getLaunchIntentForPackage("android.intent.action.navigon.START_PUBLIC");
		if (navigon != null) {
			apps.add(new NavigonApp(navigon));
		}

		Intent sygic = m_pm.getLaunchIntentForPackage("com.sygic.aura");
		if (sygic != null) {
			apps.add(new SygicApp(sygic));
		}

		apps.add(new BuildinNavigationSelector());
		return apps;
	}
}
