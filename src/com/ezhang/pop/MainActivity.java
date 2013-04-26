package com.ezhang.pop;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.ezhang.pop.model.DestinationList;
import com.ezhang.pop.model.DistanceMatrix;
import com.ezhang.pop.model.DistanceMatrixItem;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.model.FuelInfo;
import com.ezhang.pop.rest.PopRequestFactory;
import com.ezhang.pop.rest.PopRequestManager;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ListView;

public class MainActivity extends Activity implements RequestListener,
		OnClickListener {
	private PopRequestManager m_restReqManager;
	protected ArrayList<Request> m_requestList = null;
	private LocationManager m_locationManager;
	private String m_provider = null;
	private Location m_location = null;
	private String m_suburb = null;
	private List<FuelInfo> m_fuelInfoList = null;

	private static final String SAVED_DISTANCE_MATRIX_REQS = "com.ezhang.pop.saved.distance.matrix.reqs";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		m_restReqManager = PopRequestManager.from(this);
		((Button) findViewById(R.id.RefreshButtton)).setOnClickListener(this);

		if (savedInstanceState != null) {
			m_requestList = savedInstanceState
					.getParcelableArrayList(SAVED_DISTANCE_MATRIX_REQS);
		} else {
			m_requestList = new ArrayList<Request>();
		}
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
		RequestSuburb();
		if (m_fuelInfoList != null) {
			RequestDistanceMatrix(m_fuelInfoList);
		}
	}

	private void RequestSuburb() {
		if (m_location == null) {
			m_location = this.m_locationManager
					.getLastKnownLocation(m_provider);
		}
		if (m_location == null) {
			return;
		}
		if (m_suburb == null) {
			Request req = PopRequestFactory.GetCurrentSuburbRequest(m_location);
			m_requestList.add(req);
			m_restReqManager.execute(req, this);
		}
	}

	private void RequestFuelInfo() {
		if (m_suburb == null) {
			return;
		}
		Request req = PopRequestFactory.GetFuelInfoRequest(m_suburb);
		m_requestList.add(req);
		m_restReqManager.execute(req, this);
	}

	private void RequestDistanceMatrix(List<FuelInfo> fuelInfoList) {
		if (m_location == null) {
			m_location = this.m_locationManager
					.getLastKnownLocation(m_provider);
		}
		if (m_location != null) {
			DestinationList dests = new DestinationList();
			for (FuelInfo item : fuelInfoList) {
				dests.AddDestination(item.latitude, item.longitude);
			}
			String src = String.format("%s,%s", this.m_location.getLatitude(),
					this.m_location.getLongitude());
			Request req = PopRequestFactory
					.GetDistanceMatrixRequest(src, dests);
			m_requestList.add(req);
			m_restReqManager.execute(req, this);
		} else {
			// TODO: Still waiting location data.
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onRequestFinished(Request request, Bundle resultData) {
		if (m_requestList.contains(request)) {
			setProgressBarIndeterminateVisibility(false);
			m_requestList.remove(request);

			if (request.getRequestType() == PopRequestFactory.REQ_TYPE_DISTANCE_MATRIX) {
				DistanceMatrix distanceMatrix = resultData
						.getParcelable(PopRequestFactory.BUNDLE_DISTANCE_MATRIX_DATA);
				final ListView listView = (ListView)this.findViewById(R.id.listView);
				ArrayList<FuelDistanceItem> fuelDistanceItems = new ArrayList<FuelDistanceItem>();
				int i = 0;
				for(DistanceMatrixItem distanceItem: distanceMatrix.GetDistanceItems())
				{
					FuelDistanceItem item = new FuelDistanceItem();
					item.destinationAddr = distanceItem.destinationAddr;
					item.distance = distanceItem.distance;
					item.duration = distanceItem.duration;
					FuelInfo fuelInfo = this.m_fuelInfoList.get(i);
					item.price = fuelInfo.price;
					item.tradingName = fuelInfo.tradingName;
					item.latitude = fuelInfo.latitude;
					item.longitude = fuelInfo.longitude;
					fuelDistanceItems.add(item);
					i++;
				}
				Collections.sort(fuelDistanceItems, FuelDistanceItem.GetComparer());
				listView.setAdapter(new FuelListAdapter(this, fuelDistanceItems));
				listView.setOnItemClickListener(new OnItemClickListener() {
			         @Override
			         public void onItemClick(AdapterView<?> a, View v, int position, long id) {
			          Object o = listView.getItemAtPosition(position);
			          FuelDistanceItem fullObject = (FuelDistanceItem)o;
			          String url = String.format(Locale.ENGLISH,
			        		  "http://maps.google.com/maps?saddr=%s,%s&daddr=%s,%s",m_location.getLatitude(),
			        		  m_location.getLongitude(),
			        		  fullObject.latitude,
			        		  fullObject.longitude);
			          Intent i = new Intent(Intent.ACTION_VIEW, 
			        		  Uri.parse(url));
			          startActivity(i);
			          } 
			         });
			}

			if (request.getRequestType() == PopRequestFactory.REQ_TYPE_GET_CUR_SUBURB) {
				m_suburb = resultData
						.getString(PopRequestFactory.BUNDLE_CUR_SUBURB_DATA);
				RequestFuelInfo();
			}
			
			if (request.getRequestType() == PopRequestFactory.REQ_TYPE_FUEL) {
				m_fuelInfoList = resultData
						.getParcelableArrayList(PopRequestFactory.BUNDLE_FUEL_DATA);
				RequestDistanceMatrix(m_fuelInfoList);
			}
		}
	}

	@Override
	public void onRequestConnectionError(Request request, int statusCode) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRequestDataError(Request request) {
		if (m_requestList.contains(request)) {
			m_requestList.remove(request);
		}
	}

	@Override
	public void onRequestCustomError(Request request, Bundle resultData) {
		// TODO Auto-generated method stub
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putParcelableArrayList(SAVED_DISTANCE_MATRIX_REQS,
				m_requestList);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (m_locationManager == null) {
			m_locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			m_provider = GetBestProvider();
			if (m_provider != null) {
				m_locationManager.requestLocationUpdates(m_provider,
						2 * 60 * 1000L, 500.0f, this.m_locationListener);
			}
		}
		for (int i = 0; i < m_requestList.size(); i++) {
			Request request = m_requestList.get(i);

			if (m_restReqManager.isRequestInProgress(request)) {
				m_restReqManager.addRequestListener(this, request);
				setProgressBarIndeterminateVisibility(true);
			} else {
				m_restReqManager.callListenerWithCachedData(this, request);
				i--;
				m_requestList.remove(request);
			}
		}
		Refresh();
	}

	private String GetBestProvider() {
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_COARSE);
		criteria.setAltitudeRequired(false);
		criteria.setBearingRequired(false);
		criteria.setCostAllowed(true);
		criteria.setPowerRequirement(Criteria.POWER_LOW);

		String provider = m_locationManager.getBestProvider(criteria, true);
		return provider;
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (!m_requestList.isEmpty()) {
			m_restReqManager.removeRequestListener(this);
		}
	}

	private final LocationListener m_locationListener = new LocationListener() {
		public void onLocationChanged(Location location) { // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
			// log it when the location changes
			if (location != null) {
				if (m_location == null || !location.equals(m_location)) {
					m_location = location;
					m_suburb = null;
					m_fuelInfoList = null;
				}
			}
		}

		public void onProviderDisabled(String provider) {
			// Provider被disable时触发此函数，比如GPS被关闭
		}

		public void onProviderEnabled(String provider) {
			// Provider被enable时触发此函数，比如GPS被打开
		}

		public void onStatusChanged(String provider, int status, Bundle extras) {
			// Provider的转态在可用、暂时不可用和无服务三个状态直接切换时触发此函数
		}
	};
}
