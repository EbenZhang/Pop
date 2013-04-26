package com.ezhang.pop;

import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.ezhang.pop.core.StateMachine;
import com.ezhang.pop.core.StateMachine.EventAction;
import com.ezhang.pop.model.DestinationList;
import com.ezhang.pop.model.DistanceMatrix;
import com.ezhang.pop.model.DistanceMatrixItem;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.model.FuelInfo;
import com.ezhang.pop.rest.PopRequestFactory;
import com.ezhang.pop.rest.PopRequestManager;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;

public class FuelStateMachine extends Observable implements RequestListener {
	enum EmState {
		Start, GeoLocationRecieved, SuburbRecieved, FuelInfoRecieved, DistanceRecieved
	}

	enum EmEvent {
		GeoLocationEvent, SuburbEvent, FuelInfoEvent, DistanceEvent, Refresh
	}

	StateMachine<EmState, EmEvent> m_stateMachine = new StateMachine<EmState, EmEvent>(
			EmState.Start);
	public ArrayList<FuelDistanceItem> m_fuelDistanceItems = new ArrayList<FuelDistanceItem>();
	private List<FuelInfo> m_fuelInfoList = null;
	private PopRequestManager m_restReqManager;
	private LocationManager m_locationManager;
	public Location m_location = null;
	public String m_suburb = null;
	private String m_provider = null;

	public FuelStateMachine(PopRequestManager reqManager,
			LocationManager locationManager) {

		m_restReqManager = reqManager;
		m_locationManager = locationManager;

		InitStateMachineTransitions();

		m_provider = GetBestProvider();
		if (m_provider != null) {
			m_locationManager.requestLocationUpdates(m_provider,
					2 * 60 * 1000L, 500.0f, this.m_locationListener);
		}

		this.m_location = this.m_locationManager
				.getLastKnownLocation(m_provider);

		Refresh();
	}

	public void Refresh() {
		this.m_stateMachine.HandleEvent(EmEvent.Refresh, null);
	}

	private void InitStateMachineTransitions() {
		m_stateMachine.AddTransition(EmState.Start,
				EmState.GeoLocationRecieved, EmEvent.GeoLocationEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						RequestSuburb();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.Start, EmState.Start,
				EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
						if (m_location == null) {
							Notify();
							return;
						}
						if (m_suburb == null) {
							m_stateMachine.SetState(EmState.GeoLocationRecieved);
							RequestSuburb();
						} else {
							m_stateMachine.SetState(EmState.SuburbRecieved);
							RequestFuelInfo();
						}
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.GeoLocationRecieved,
				EmState.SuburbRecieved, EmEvent.SuburbEvent, new EventAction() {
					public void PerformAction(Bundle param) {
						m_suburb = param
								.getString(PopRequestFactory.BUNDLE_CUR_SUBURB_DATA);
						RequestFuelInfo();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.GeoLocationRecieved,
				EmState.GeoLocationRecieved, EmEvent.Refresh,
				new EventAction() {
					public void PerformAction(Bundle param) {
						if (m_suburb == null) {
							RequestSuburb();
						} else {
							m_stateMachine.SetState(EmState.SuburbRecieved);
							RequestFuelInfo();
						}
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.SuburbRecieved,
				EmState.FuelInfoRecieved, EmEvent.FuelInfoEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						m_fuelInfoList = param
								.getParcelableArrayList(PopRequestFactory.BUNDLE_FUEL_DATA);
						RequestDistanceMatrix(m_fuelInfoList);
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.SuburbRecieved,
				EmState.SuburbRecieved, EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
						RequestFuelInfo();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.SuburbRecieved,
				EmState.GeoLocationRecieved, EmEvent.GeoLocationEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						RequestSuburb();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.FuelInfoRecieved,
				EmState.DistanceRecieved, EmEvent.DistanceEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						DistanceMatrix distanceMatrix = param
								.getParcelable(PopRequestFactory.BUNDLE_DISTANCE_MATRIX_DATA);
						OnDistanceMatrixRecieved(distanceMatrix);
						Notify();
					}
				});
		m_stateMachine.AddTransition(EmState.FuelInfoRecieved,
				EmState.SuburbRecieved, EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
						RequestFuelInfo();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.FuelInfoRecieved,
				EmState.GeoLocationRecieved, EmEvent.GeoLocationEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						RequestSuburb();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.DistanceRecieved,
				EmState.GeoLocationRecieved, EmEvent.GeoLocationEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						RequestSuburb();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.DistanceRecieved,
				EmState.SuburbRecieved, EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
						RequestFuelInfo();
						Notify();
					}
				});
	}

	private void OnDistanceMatrixRecieved(DistanceMatrix distanceMatrix) {
		m_fuelDistanceItems.clear();
		int i = 0;
		for (DistanceMatrixItem distanceItem : distanceMatrix
				.GetDistanceItems()) {
			FuelDistanceItem item = new FuelDistanceItem();
			item.destinationAddr = distanceItem.destinationAddr;
			item.distance = distanceItem.distance;
			item.duration = distanceItem.duration;
			FuelInfo fuelInfo = this.m_fuelInfoList.get(i);
			item.price = fuelInfo.price;
			item.tradingName = fuelInfo.tradingName;
			item.latitude = fuelInfo.latitude;
			item.longitude = fuelInfo.longitude;
			m_fuelDistanceItems.add(item);
			i++;
		}
		Notify();
	}

	@Override
	public void onRequestFinished(Request request, Bundle resultData) {
		if (request.getRequestType() == PopRequestFactory.REQ_TYPE_DISTANCE_MATRIX) {
			this.m_stateMachine.HandleEvent(EmEvent.DistanceEvent, resultData);
		}

		if (request.getRequestType() == PopRequestFactory.REQ_TYPE_GET_CUR_SUBURB) {
			this.m_stateMachine.HandleEvent(EmEvent.SuburbEvent, resultData);
		}

		if (request.getRequestType() == PopRequestFactory.REQ_TYPE_FUEL) {
			this.m_stateMachine.HandleEvent(EmEvent.FuelInfoEvent, resultData);
		}
	}

	@Override
	public void onRequestConnectionError(Request request, int statusCode) {
	}

	@Override
	public void onRequestDataError(Request request) {
	}

	@Override
	public void onRequestCustomError(Request request, Bundle resultData) {
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
			m_restReqManager.execute(req, this);
		}
	}

	private void RequestFuelInfo() {
		if (m_suburb == null) {
			return;
		}
		Request req = PopRequestFactory.GetFuelInfoRequest(m_suburb);
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
			m_restReqManager.execute(req, this);
		} else {
			// TODO: Still waiting location data.
		}
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

	private final LocationListener m_locationListener = new LocationListener() {
		public void onLocationChanged(Location location) { // 当坐标改变时触发此函数，如果Provider传进相同的坐标，它就不会被触发
			// log it when the location changes
			if (location != null) {
				if (m_location == null || !location.equals(m_location)) {
					m_location = location;
					m_suburb = null;
					m_fuelInfoList = null;
					m_fuelDistanceItems.clear();
					m_stateMachine.HandleEvent(EmEvent.GeoLocationEvent, null);
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

	public EmState GetCurState() {
		// TODO Auto-generated method stub
		return this.m_stateMachine.GetState();
	}
	private void Notify()
	{
		setChanged();
		notifyObservers();
	}
}
