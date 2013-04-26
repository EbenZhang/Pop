package com.ezhang.pop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;

import com.ezhang.pop.FuelStateMachine.EmState;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.rest.PopRequestManager;

import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends Activity implements OnClickListener, Observer {
	private PopRequestManager m_restReqManager;
	private LocationManager m_locationManager;
	private FuelStateMachine m_fuelStateMachine;
	private ArrayList<FuelDistanceItem> m_fuelInfoList = new ArrayList<FuelDistanceItem>();
	private ListView m_listView = null;
	private static final String SAVED_DISTANCE_MATRIX_REQS = "com.ezhang.pop.saved.distance.matrix.reqs";
	Button m_refreshButton = null;
	AnimationDrawable m_refreshButtonAnimation = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		m_restReqManager = PopRequestManager.from(this);
		m_refreshButton = (Button) findViewById(R.id.RefreshButtton);
		m_refreshButton.setOnClickListener(this);
		m_refreshButtonAnimation = (AnimationDrawable)m_refreshButton.getBackground();
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

	@Override
	public void onClick(View view) {
		switch (view.getId()) {
		case R.id.RefreshButtton:
			Refresh();
			break;
		}
	}

	private void Refresh() {
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
		if (m_fuelStateMachine == null) {
			m_fuelStateMachine = new FuelStateMachine(this.m_restReqManager,
					this.m_locationManager);
			m_fuelStateMachine.addObserver(this);
		} else {
			m_fuelStateMachine.Refresh();
		}
		m_refreshButton.setEnabled(false);
		m_refreshButtonAnimation.start();
	}

	@Override
	protected void onPause() {
		super.onPause();
		// TODO
	}

	@Override
	public void update(Observable arg0, Object arg1) {
		TextView statusText = (TextView)this.findViewById(R.id.statusText);
		if (this.m_fuelStateMachine.GetCurState() == EmState.DistanceRecieved) {
			this.m_fuelInfoList.clear();
			this.m_fuelInfoList
					.addAll(this.m_fuelStateMachine.m_fuelDistanceItems);
			Collections.sort(m_fuelInfoList,
					FuelDistanceItem.GetComparer());
			statusText.setText("Completed");
			((BaseAdapter) m_listView.getAdapter()).notifyDataSetChanged();
			m_refreshButtonAnimation.stop();
			m_refreshButton.setEnabled(true);

		}
		if (this.m_fuelStateMachine.GetCurState() == EmState.SuburbRecieved) {
			statusText.setText("Suburb Recieved: " + this.m_fuelStateMachine.m_suburb);
			m_refreshButtonAnimation.start();
			m_refreshButton.setEnabled(false);
		}
		
		if (this.m_fuelStateMachine.GetCurState() == EmState.FuelInfoRecieved) {
			statusText.setText("Fuel Price Info Recieved");
			m_refreshButtonAnimation.start();
			m_refreshButton.setEnabled(false);
		}
		
		if (this.m_fuelStateMachine.GetCurState() == EmState.Start) {
			statusText.setText("Waiting For Location Information");
			m_refreshButtonAnimation.start();
			m_refreshButton.setEnabled(false);
		}
		
		if (this.m_fuelStateMachine.GetCurState() == EmState.GeoLocationRecieved) {
			statusText.setText("Location Information Recieved");
			m_refreshButtonAnimation.start();
			m_refreshButton.setEnabled(false);
		}
	}
}
