package com.ezhang.pop.navigation;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;

public class SygicApp extends NavigationApp {
	Intent m_intent;
	public SygicApp(Intent intent)
	{
		super("Sygic");
		m_intent = intent;
	}
	@Override
	public void CallNavigateApp(Activity activity, String srcLatitude,
			String srcLongitude, String destLatitudue, String destLongitude) {
		String str = String.format("http://com.sygic.aura/coordinate|%s|%s|drive", destLongitude, destLatitudue);
		
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(str));
		ComponentName cn = m_intent.getComponent();
		String pn = cn.getPackageName();
		String sn  = cn.getClassName();
		i.setClassName(pn, sn);
		activity.startActivity(i);
	}

}
