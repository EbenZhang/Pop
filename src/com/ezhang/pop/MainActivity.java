package com.ezhang.pop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import com.ezhang.pop.FuelStateMachine.EmEvent;
import com.ezhang.pop.FuelStateMachine.EmState;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.rest.PopRequestManager;

import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements Observer {
	private PopRequestManager m_restReqManager;
	private LocationManager m_locationManager;
	private FuelStateMachine m_fuelStateMachine;
	private ArrayList<FuelDistanceItem> m_fuelInfoList = new ArrayList<FuelDistanceItem>();
	private ListView m_listView = null;
	private static final String SAVED_DISTANCE_MATRIX_REQS = "com.ezhang.pop.saved.distance.matrix.reqs";
	Button m_refreshButton = null;
	AnimationDrawable m_refreshButtonAnimation = null;
	TextView m_statusText = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		m_restReqManager = PopRequestManager.from(this);
		m_refreshButton = (Button) findViewById(R.id.RefreshButtton);
		m_refreshButtonAnimation = (AnimationDrawable) m_refreshButton
				.getBackground();
		m_statusText = (TextView) this.findViewById(R.id.statusText);
		if (savedInstanceState != null) {
			ArrayList<FuelDistanceItem> cached = savedInstanceState
					.getParcelableArrayList(SAVED_DISTANCE_MATRIX_REQS);
			m_fuelInfoList.addAll(cached);
		}

		m_listView = (ListView) this.findViewById(R.id.listView);
		m_listView.setAdapter(new FuelListAdapter(this, m_fuelInfoList));
		m_listView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> a, View v, int position,
					long id) {
				Object o = m_listView.getItemAtPosition(position);
				FuelDistanceItem fullObject = (FuelDistanceItem) o;
				if (m_fuelStateMachine != null
						&& m_fuelStateMachine.m_location != null) {
					String url = String.format(Locale.ENGLISH,

					"http://maps.google.com/maps?saddr=%s,%s&daddr=%s,%s",
							m_fuelStateMachine.m_location.getLatitude(),
							m_fuelStateMachine.m_location.getLongitude(),
							fullObject.latitude, fullObject.longitude);
					Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
					startActivity(i);
				}
			}
		});
	}

	public void Refresh(View v) {
		this.m_fuelStateMachine.Refresh();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putParcelableArrayList(SAVED_DISTANCE_MATRIX_REQS,
				m_fuelInfoList);
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (m_locationManager == null) {
			m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		}

		ConnectivityManager nInfo = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo network = nInfo.getActiveNetworkInfo();
		boolean hasNetwork = false;
		if (network != null) {
			hasNetwork = network.isConnectedOrConnecting();
		}
		if (!hasNetwork) {

			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

			// Setting Dialog Title
			alertDialog.setTitle("Network Access Required");

			// Setting Dialog Message
			alertDialog
					.setMessage("Network is not enabled. Press either enable Wi-Fi or Mobile Network Data");

			// On pressing Settings button
			alertDialog.setPositiveButton("Wi-Fi",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(
									Settings.ACTION_WIFI_SETTINGS);
							MainActivity.this.startActivity(intent);
						}
					});

			// On pressing Settings button
			alertDialog.setNegativeButton("MobileNetwork",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
							//intent.setClassName("com.android.phone", "com.android.phone.Settings");
							MainActivity.this.startActivity(intent);
						}
					});

			// Showing Alert Message
			alertDialog.show();
			return;
		}

		boolean isGPSEnabled = m_locationManager
				.isProviderEnabled(LocationManager.GPS_PROVIDER);
		boolean isNetworkLocationEnabled = m_locationManager
				.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

		boolean isOnSimulator = Build.FINGERPRINT.startsWith("generic");
		isNetworkLocationEnabled |= isOnSimulator; // network location is not
													// supported by simulator.

		if (!isGPSEnabled || !isNetworkLocationEnabled) {
			AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

			// Setting Dialog Title
			alertDialog.setTitle("Locaion Access Required");

			// Setting Dialog Message
			alertDialog
					.setMessage("Locaion Access is not enabled. Press go to the settings menu and enbale both \n * GPS satellites\n * Wi-Fi & mobile network");

			// On pressing Settings button
			alertDialog.setPositiveButton("Go",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(
									Settings.ACTION_LOCATION_SOURCE_SETTINGS);
							MainActivity.this.startActivity(intent);
						}
					});

			// Showing Alert Message
			alertDialog.show();
			return;
		}

		if (m_fuelStateMachine == null) {
			m_fuelStateMachine = new FuelStateMachine(this.m_restReqManager,
					this.m_locationManager);
			m_fuelStateMachine.addObserver(this);
		} else {
			m_fuelStateMachine.Refresh();
		}
		m_statusText.setText("Waiting For Location Information");
		SwitchToWaitingStatus();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// TODO
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		if (this.m_fuelStateMachine.GetCurState() == EmState.DistanceRecieved) {
			this.m_fuelInfoList.clear();
			this.m_fuelInfoList
					.addAll(this.m_fuelStateMachine.m_fuelDistanceItems);
			Collections.sort(m_fuelInfoList, FuelDistanceItem.GetComparer());
			m_statusText.setText("Completed");
			((BaseAdapter) m_listView.getAdapter()).notifyDataSetChanged();
			SwitchToStopWaiting();

		}
		if (this.m_fuelStateMachine.GetCurState() == EmState.SuburbRecieved) {
			m_statusText.setText("Suburb Recieved: "
					+ this.m_fuelStateMachine.m_suburb);
			SwitchToWaitingStatus();
		}

		if (this.m_fuelStateMachine.GetCurState() == EmState.FuelInfoRecieved) {
			m_statusText.setText("Fuel Price Info Recieved");
			SwitchToWaitingStatus();
		}

		if (this.m_fuelStateMachine.GetCurState() == EmState.Start) {
			m_statusText.setText("Waiting For Location Information");
			SwitchToWaitingStatus();
		}

		if (this.m_fuelStateMachine.GetCurState() == EmState.GeoLocationRecieved) {
			m_statusText.setText("Location Information Recieved");
			SwitchToWaitingStatus();
		}

		if (this.m_fuelStateMachine.GetCurState() == EmState.Timeout) {
			if (this.m_fuelStateMachine.m_timeoutEvent == EmEvent.GeoLocationEvent) {
				m_statusText.setText("Unable To Get Location");
			}
			this.SwitchToStopWaiting();
			m_statusText.setVisibility(View.VISIBLE);
		}
	}

	private void SwitchToStopWaiting() {
		m_refreshButtonAnimation.stop();
		m_refreshButton.setEnabled(true);
		m_statusText.setVisibility(View.GONE);
	}

	private void SwitchToWaitingStatus() {
		m_refreshButtonAnimation.start();
		m_refreshButton.setEnabled(false);
		m_statusText.setVisibility(View.VISIBLE);
	}

	public void ShareWithFriend(View v) {
		String content = "Hey, here is an awesome app that can list the comparison fuel price of the nearby stations.";
		Intent shareIntent = new Intent(Intent.ACTION_SEND);
		shareIntent.setType("text/plain");
		shareIntent.putExtra(Intent.EXTRA_TEXT, content);
		startActivity(shareIntent);
	}
}
