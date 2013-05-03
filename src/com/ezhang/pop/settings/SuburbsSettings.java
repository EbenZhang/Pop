package com.ezhang.pop.settings;

import com.ezhang.pop.R;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SuburbsSettings {
	private final Context m_context;
	SharedPreferences m_settings = null;
	public SuburbsSettings(Context context) {
		m_context = context;
		m_settings = PreferenceManager.getDefaultSharedPreferences(m_context);
	}
	public boolean IncludeSurroundings()
	{
		return m_settings.getBoolean(m_context.getString(R.string.surrounding_suburbs_settings_key), true);
	}
}
