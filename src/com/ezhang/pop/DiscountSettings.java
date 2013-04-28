package com.ezhang.pop;

import com.ezhang.pop.core.ICallable;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

public class DiscountSettings {
	private static final String PREFS_NAME = "com.ezhang.pop.discount.preference";
	private static final String HAS_DISCOUNT_SETTINGS = "com.ezhang.pop.discount.has.discount.settings";
	private static final String WWS_DISCOUNT_SETTINGS = "com.ezhang.pop.discount.wws";
	private static final String COLES_DISCOUNT_SETTINGS = "com.ezhang.pop.discount.coles";
	private final Context m_context;
	public int m_colesDiscount = 0;
	public int m_wwsDiscount = 0;
	SharedPreferences m_settings = null;

	public DiscountSettings(Context context) {
		m_context = context;
		m_settings = this.m_context.getSharedPreferences(
				PREFS_NAME, 0);
	}

	public void LoadSettings(final ICallable<Object,Object> callable) {
		boolean hasDiscountSettings = m_settings.getBoolean(
				HAS_DISCOUNT_SETTINGS, false);
		if (hasDiscountSettings) {
			m_wwsDiscount = m_settings.getInt(WWS_DISCOUNT_SETTINGS, 0);
			m_colesDiscount = m_settings.getInt(COLES_DISCOUNT_SETTINGS, 0);
			callable.Call(null);
		} else {
			ShowSettingsDialog(callable);
		}
	}

	public void ShowSettingsDialog(final ICallable<Object, Object> callable) {
		LayoutInflater factory = LayoutInflater.from(this.m_context);
		
		final View view = factory.inflate(R.layout.discount_settings, null);
		((EditText)view.findViewById(R.id.coles_voucher)).setText(String.valueOf(m_colesDiscount));
		((EditText)view.findViewById(R.id.wws_voucher)).setText(String.valueOf(m_wwsDiscount));
		AlertDialog discountSettingsDlg = new AlertDialog.Builder(m_context)
				.setTitle("Discount Settings").setView(view).setCancelable(true)
				.setOnCancelListener(new OnCancelListener()
				{
					@Override
					public void onCancel(DialogInterface arg0) {
						String text = ((EditText)view.findViewById(R.id.coles_voucher)).getText().toString();
						m_colesDiscount = GetIntFromText(text);
						
						text = ((EditText)view.findViewById(R.id.wws_voucher)).getText().toString();
						m_wwsDiscount = GetIntFromText(text);
						
						Editor editor = m_settings.edit();
						editor.putBoolean(HAS_DISCOUNT_SETTINGS, true);
						editor.putInt(WWS_DISCOUNT_SETTINGS, m_wwsDiscount);
						editor.putInt(COLES_DISCOUNT_SETTINGS, m_colesDiscount);
						editor.commit();
						
						callable.Call(null);						
					}
				}
				).create();
		
		discountSettingsDlg.show();
	}

	private int GetIntFromText(String text) {
		if(text != "")
		{
			return Integer.parseInt(text);
		}
		else
		{
			return 0;
		}
	}
}
