package com.ezhang.pop.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
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
import com.ezhang.pop.network.RequestFactory;
import com.ezhang.pop.network.RequestManager;
import com.ezhang.pop.settings.AppSettings;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CustomLocationActivity extends Activity implements RequestListener {

    private static class TimerHandler extends Handler {
        CustomLocationActivity m_activity;
        public TimerHandler(CustomLocationActivity activity) {
            m_activity = activity;
        }

        @Override
        public void handleMessage(Message msg) {
            m_activity.OnTimeout(msg);
        }
    }

	private static final int MAX_HISTORY_LOCATION = 6;
    private static final int MSG_GPS_TIMEOUT = 1;
    private static final int MSG_GEO_TO_ADDR_TIMEOUT = 2;
    private static final int MSG_WIFI_TIMEOUT = 3;

    private LocationManager m_locationManager;

    private Location m_location = null;

	private int m_selectedHistoryLocation = 0;

	private List<String> m_locations = null;

	private AppSettings m_settings = null;

    private ProgressDialog m_progress;

    private RequestManager m_reqManager;

    private Timer m_timer = null;
    private TimerTask m_timerTask = null;
    private final Handler m_timeoutHandler = new TimerHandler(this);

    private AlertDialog m_locationAccessDlg = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_custom_location);

        m_progress = new ProgressDialog(this);
        m_progress.setCancelable(true);
        m_progress.dismiss();
        m_progress.setMessage("Detecting. It may take a long time...");

        m_reqManager = RequestManager.from(this);

        m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		
		m_settings = new AppSettings(this);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
            this, R.array.suburbs, android.R.layout.simple_list_item_1);
        AutoCompleteTextView suburbSelector = (AutoCompleteTextView) findViewById(R.id.autoCompleteSuburb);
        suburbSelector.setAdapter(adapter);

        suburbSelector.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AutoSelectCustomRadioBtn();
                return false;
            }
        });

        suburbSelector.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                AutoSelectCustomRadioBtn();
                return false;
            }
        });

		EditText streetAddr = (EditText) this.findViewById(R.id.streetAddress);
		streetAddr.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				AutoSelectCustomRadioBtn();
				return false;
			}
		});

		streetAddr.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				AutoSelectCustomRadioBtn();
			}

			@Override
			public void afterTextChanged(Editable arg0) {
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}
		});

		m_locations = m_settings.GetHistoryLocations();

        final RadioGroup group = (RadioGroup) this
                .findViewById(R.id.locationRadioGroup);
		RadioButton firstLocationRadioBtn = null;
		for (int i = 0; i < m_locations.size(); ++i) {
			RadioButton locationRadioBtn = new RadioButton(this);
			final int index = i;
            locationRadioBtn.setId(index);
			locationRadioBtn.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					m_selectedHistoryLocation = index;
				}
			});
			locationRadioBtn.setText(m_locations.get(i));
			group.addView(locationRadioBtn, i);
			if (i == 0) {
				firstLocationRadioBtn = locationRadioBtn;
			}
		}
		if (firstLocationRadioBtn != null) {
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
            boolean existing = ((ArrayAdapter<String>)suburbSelector.getAdapter()).getPosition(suburb.toUpperCase()) != -1;
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

		if (!addNewLocation) {
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

	private void AutoSelectCustomRadioBtn() {
        final RadioGroup group = (RadioGroup) this
                .findViewById(R.id.locationRadioGroup);
		group.check(R.id.radioBtnCustomLocation);
	}

    public void OnClickDetectByGPS(View v) {
        AutoSelectCustomRadioBtn();
        EmIndication indication = PromptEnableLocationService(LocationManager.GPS_PROVIDER);
        if (indication == EmIndication.EmContinue) {
            m_locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    60 * 1000L, 20.0f, this.m_locationListener);

            Message msg = new Message();
            msg.what = MSG_GPS_TIMEOUT;
            StartTimer(25000, msg);
            m_progress.show();
        }
    }

    public void OnClickDetectByWifi(View v) {
        AutoSelectCustomRadioBtn();
        EmIndication indication = PromptEnableLocationService(LocationManager.NETWORK_PROVIDER);
        if (indication == EmIndication.EmContinue) {
            m_locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    60 * 1000L, 20.0f, this.m_locationListener);

            Message msg = new Message();
            msg.what = MSG_WIFI_TIMEOUT;
            StartTimer(25000, msg);
            m_progress.show();
        }
    }

    private final LocationListener m_locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            // log it when the location changes
            if (location != null) {
                if (m_location == null || !location.equals(m_location)) {
                    StopTimer();
                    m_location = location;
                    m_locationManager.removeUpdates(this);
                    RequestSuburb();
                }
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
    };

    private void StopTimer() {
        if (m_timer != null) {
            m_timer.cancel();
            m_timer = null;
        }

        if (m_timerTask != null) {
            m_timerTask.cancel();
            m_timerTask = null;
        }
    }

    private void StartTimer(int millSeconds, final Message msg) {
        StopTimer();

        if (m_timer == null) {
            m_timer = new Timer();
        }

        if (m_timerTask == null) {
            m_timerTask = new TimerTask() {
                public void run() {
                    m_timeoutHandler.sendMessage(msg);
                }
            };
        }

        if (m_timer != null && m_timerTask != null) {
            m_timer.schedule(m_timerTask, millSeconds);
        }
    }

    private void RequestSuburb() {
        Message msg = new Message();
        msg.what = MSG_GEO_TO_ADDR_TIMEOUT;
        StartTimer(5000, msg);
        Request req = RequestFactory.GetCurrentSuburbRequest(m_location);
        m_reqManager.execute(req, this);
    }

    @Override
    public void onRequestFinished(Request request, Bundle bundle) {
        StopTimer();
        String suburb = bundle
                .getString(RequestFactory.BUNDLE_CUR_SUBURB_DATA);
        String address = bundle
                .getString(RequestFactory.BUNDLE_CUR_ADDRESS_DATA);

        AutoCompleteTextView suburbSelector = (AutoCompleteTextView) findViewById(R.id.autoCompleteSuburb);
        suburbSelector.setText(suburb);
        EditText streetAddrEditText = (EditText) this.findViewById(R.id.streetAddress);
        streetAddrEditText.setText(LocationSpliter.Split(address).first);
        m_progress.dismiss();
    }

    @Override
    public void onRequestConnectionError(Request request, int i) {

    }

    @Override
    public void onRequestDataError(Request request) {

    }

    @Override
    public void onRequestCustomError(Request request, Bundle bundle) {

    }

    public void OnTimeout(Message msg){
        if(msg.what == MSG_GPS_TIMEOUT || msg.what == MSG_WIFI_TIMEOUT){
            String LocateType = msg.what == MSG_GPS_TIMEOUT ? LocationManager.GPS_PROVIDER : LocationManager.NETWORK_PROVIDER;
            m_location = m_locationManager.getLastKnownLocation(LocateType);
            m_locationManager.removeUpdates(m_locationListener);
            if (m_location == null) {
                m_progress.dismiss();
                Toast.makeText(this,
                        "Unable to get your location. Please check if the location service is enabled.",
                        Toast.LENGTH_LONG).show();
            } else {
                RequestSuburb();
            }
        }else if(msg.what == MSG_GEO_TO_ADDR_TIMEOUT){
            Toast.makeText(this,
                    "Unable to get your location. Please check if your internet service is enabled.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private EmIndication PromptEnableLocationService(String provider) {
        boolean isGPSEnabled = m_locationManager
                .isProviderEnabled(provider);

        if (!isGPSEnabled) {
            CreateLocationAccessAlertDlg();
            if (!m_locationAccessDlg.isShowing()) {
                m_locationAccessDlg.show();
            }
            return EmIndication.EmStop;
        }

        return EmIndication.EmContinue;
    }

    private void CreateLocationAccessAlertDlg() {
        if (m_locationAccessDlg != null) {
            return;
        }
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Location Access Required");

        // Setting Dialog Message
        alertDialog
                .setMessage("Location Access is disabled. Press go to your phone settings and enable it");

        // On pressing Settings button
        alertDialog.setPositiveButton("Go",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(
                                Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        CustomLocationActivity.this.startActivity(intent);
                    }
                });

        // Showing Alert Message
        m_locationAccessDlg = alertDialog.create();
    }
    @Override
    protected void onPause() {
        StopTimer();
        super.onPause();
    }
}
