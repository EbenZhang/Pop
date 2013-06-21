package com.ezhang.pop.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.View.OnTouchListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.ezhang.pop.R;
import com.ezhang.pop.core.LocationSpliter;
import com.ezhang.pop.core.NotEmptyValidator;
import com.ezhang.pop.settings.AppSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CustomLocationActivity extends Activity {
	private static final int MAX_HISTORY_LOCATION = 4;

	private RadioButton m_btnGPS = null;

	private int m_selectedHistoryLocation = 0;

	private List<String> m_locations = null;

	private AppSettings m_settings = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_custom_location);
		
		m_settings = new AppSettings(this);
		
		final RadioGroup group = (RadioGroup) this
				.findViewById(R.id.locationRadioGroup);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.suburbs, android.R.layout.simple_list_item_1);
        AutoCompleteTextView suburbSelector = (AutoCompleteTextView) findViewById(R.id.autoCompleteSuburb);
        suburbSelector.setAdapter(adapter);

        suburbSelector.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AutoSelectCustomRadioBtn(group);
                return false;
            }
        });

        suburbSelector.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                AutoSelectCustomRadioBtn(group);
                return false;
            }
        });

		EditText streetAddr = (EditText) this.findViewById(R.id.streetAddress);
		streetAddr.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				AutoSelectCustomRadioBtn(group);
				return false;
			}
		});

		streetAddr.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				AutoSelectCustomRadioBtn(group);
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
		});

		m_btnGPS = (RadioButton) this.findViewById(R.id.radioBtnGPSLocation);

		boolean gpsAsLocation = m_settings.UseGPSAsLocation();
		if (gpsAsLocation) {
			m_btnGPS.setChecked(true);
		}

		m_locations = m_settings.GetHistoryLocations();

		RadioButton firstLocationRadioBtn = null;
		for (int i = 0; i < m_locations.size(); ++i) {
			RadioButton locationRadioBtn = new RadioButton(this);
			final int index = i;
			locationRadioBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					m_selectedHistoryLocation = index;
				}
			});
			locationRadioBtn.setText(m_locations.get(i));
			group.addView(locationRadioBtn, i + 1);// +1 for GPS radio button
			if (i == 0) {
				firstLocationRadioBtn = locationRadioBtn;
			}
		}
		if (!gpsAsLocation && firstLocationRadioBtn != null) {
			firstLocationRadioBtn.setChecked(true);
		}

        String lastSuburb = m_settings.GetLastSuburb();
        if(!lastSuburb.equals("")){
            suburbSelector.setText(lastSuburb);
        }else{
            suburbSelector.setText(adapter.getItem(0));
        }
	}

	public void OnOKClicked(View v) {

		RadioGroup group = (RadioGroup) this
				.findViewById(R.id.locationRadioGroup);
		int selectedID = group.getCheckedRadioButtonId();

		boolean addNewLocation = selectedID == R.id.radioBtnCustomLocation;
		if (addNewLocation) {
            AutoCompleteTextView suburbSelector = (AutoCompleteTextView) findViewById(R.id.autoCompleteSuburb);
            String suburb = suburbSelector.getText().toString();
            boolean existing = ((ArrayAdapter<String>)suburbSelector.getAdapter()).getPosition(suburb) != -1;
            if(!existing){
                Toast.makeText(this, "Cannot find suburb: '" + suburb + "'", Toast.LENGTH_SHORT).show();
                suburbSelector.requestFocus();
                return;
            }
            EditText streetAddrEditText = (EditText) this.findViewById(R.id.streetAddress);
            String streetAddr = streetAddrEditText.getText().toString();
            boolean notEmpty = NotEmptyValidator.NotEmpty(this, streetAddr, "Please input your address.");
            if(!notEmpty){
                streetAddrEditText.requestFocus();
                return;
            }
            String newLocation = GenerateNewLocationString(streetAddr, suburb);
			AddNewLocation(newLocation);
            m_settings.SetLastSuburb(suburb);
		}

        boolean useGPS = selectedID == R.id.radioBtnGPSLocation;
        this.m_settings.UseGPSAsLocation(useGPS);
		if (!addNewLocation && !useGPS) {
			ReorderHistoryLocations();
		}

		SaveLocations();
		this.finish();
	}

	private void SaveLocations() {
		this.m_settings.SaveLocations(m_locations);
	}

	private void AddNewLocation(String newLocation) {
		boolean alreadyExisting = false;
		if (m_locations == null) {
			m_locations = new ArrayList<String>();
		}
		for (String l : this.m_locations) {
			if (l.toLowerCase(Locale.ENGLISH).equals(
					newLocation.toLowerCase(Locale.ENGLISH))) {
				alreadyExisting = true;
				break;
			}
		}
		if (!alreadyExisting) {
			this.m_locations.add(0, newLocation);
		}
		if (m_locations.size() > MAX_HISTORY_LOCATION) {
            m_locations = m_locations.subList(0, MAX_HISTORY_LOCATION);
		}
	}

	private String GenerateNewLocationString(String streetAddr, String suburb) {
		return LocationSpliter.Combine(streetAddr, suburb);
	}

	private void ReorderHistoryLocations() {
		if (this.m_selectedHistoryLocation != 0) {
			String selected = m_locations.get(this.m_selectedHistoryLocation);
			m_locations.remove(m_selectedHistoryLocation);
			m_locations.add(0, selected);
		}
	}

	public void OnCancelClicked(View v) {
		this.finish();
	}

	private void AutoSelectCustomRadioBtn(final RadioGroup group) {
		group.check(R.id.radioBtnCustomLocation);
	}
}
