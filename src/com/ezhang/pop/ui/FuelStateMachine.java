package com.ezhang.pop.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.ezhang.pop.core.LocationSpliter;
import com.ezhang.pop.core.StateMachine;
import com.ezhang.pop.core.StateMachine.EventAction;
import com.ezhang.pop.core.TimeUtil;
import com.ezhang.pop.model.CacheManager;
import com.ezhang.pop.model.DestinationList;
import com.ezhang.pop.model.DistanceMatrix;
import com.ezhang.pop.model.DistanceMatrixItem;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.model.FuelInfo;
import com.ezhang.pop.network.RequestFactory;
import com.ezhang.pop.network.RequestManager;
import com.ezhang.pop.settings.AppSettings;
import com.ezhang.pop.utils.PriceDate;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.requestmanager.RequestManager.RequestListener;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Observable;
import java.util.Timer;
import java.util.TimerTask;

public class FuelStateMachine extends Observable implements RequestListener {
    private boolean m_paused = false;

    public void LoadFromCacheFile() {
        m_cacheManager.LoadFromFile();
    }

    private static class TimerHandler extends Handler {
		FuelStateMachine m_fuelStateMachine = null;

		public TimerHandler(FuelStateMachine fuelStateMachine) {
			m_fuelStateMachine = fuelStateMachine;
		}

		@Override
		public void handleMessage(Message msg) {
			m_fuelStateMachine.m_stateMachine
					.HandleEvent(EmEvent.Timeout, null);
		}
	}

	enum EmState {
		Invalid, Start, FuelInfoReceived, DistanceReceived, Timeout
	}

	enum EmEvent {
		Invalid, GeoLocationEvent, SuburbEvent, FuelInfoEvent, DistanceEvent, Refresh, Timeout
	}

    private final StateMachine<EmState, EmEvent> m_stateMachine = new StateMachine<EmState, EmEvent>(
			EmState.Start);
	public List<FuelDistanceItem> m_fuelDistanceItems = new ArrayList<FuelDistanceItem>();
	private List<FuelInfo> m_fuelInfoList = null;
	private final RequestManager m_restReqManager;
	public String m_suburb = null;
	public String m_address = null;
	public EmEvent m_timeoutEvent = EmEvent.Invalid;
	private Timer m_timer = null;
    private TimerTask m_timerTask = null;
    private final Handler m_timeoutHandler = new TimerHandler(this);
    private AppSettings m_settings = null;

    private final CacheManager m_cacheManager;

    private EmState m_lastState = EmState.Invalid;
    private boolean m_fuelInfoChanged = false;
    private boolean m_fuelDistanceInfoChanged = false;

    private PriceDate m_dateOfFuel = PriceDate.Today;

	public FuelStateMachine(RequestManager reqManager, AppSettings settings, String externalCacheDir) {
		m_restReqManager = reqManager;
		m_settings = settings;
        m_cacheManager = new CacheManager(externalCacheDir);

		InitStateMachineTransitions();
	}

    public void Refresh() {
        m_paused = false;
        m_address = this.m_settings.GetHistoryLocations().get(0);
        m_suburb = LocationSpliter.Split(m_address).second;
        this.m_stateMachine.HandleEvent(EmEvent.Refresh, null);
	}

    private void StartTimer(int millSeconds) {
		StopTimer();

		if (m_timer == null) {
			m_timer = new Timer();
		}

		if (m_timerTask == null) {
			m_timerTask = new TimerTask() {
				public void run() {
					m_timeoutHandler.sendMessage(new Message());
				}
			};
		}

		if (m_timer != null && m_timerTask != null) {
			m_timer.schedule(m_timerTask, millSeconds);
		}
	}

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

	private void InitStateMachineTransitions() {
		m_stateMachine.AddTransition(EmState.Start, EmState.Start,
				EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
                        StopTimer();
                        Start();
					}
				});

		m_stateMachine.AddTransition(EmState.Start, EmState.Timeout,
				EmEvent.Timeout, new EventAction() {
					public void PerformAction(Bundle param) {
						m_timeoutEvent = EmEvent.FuelInfoEvent;
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.Timeout, EmState.Start,
				EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
                        StopTimer();
                        Start();
					}
				});

		m_stateMachine.AddTransition(EmState.Start,
				EmState.FuelInfoReceived, EmEvent.FuelInfoEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
                        StopTimer();
                        List<FuelInfo> fuelInfo = param
                                .getParcelableArrayList(RequestFactory.BUNDLE_FUEL_DATA);
                        String publishDate = param
                                .getString(RequestFactory.BUNDLE_FUEL_INFO_PUBLISH_DATE);

                        Date publishDay = new Date();
                        if(publishDate != null && !publishDate.equals("")){
                            try {
                                DateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                                publishDay = format.parse(publishDate);
                            }catch (ParseException ex){
                                ex.printStackTrace();
                            }
                        }
                        OnFuelInfoReceived(fuelInfo, publishDay, false);
					}
				});

		m_stateMachine.AddTransition(EmState.FuelInfoReceived,
				EmState.DistanceReceived, EmEvent.DistanceEvent,
				new EventAction() {
					public void PerformAction(Bundle param) {
						StopTimer();
						DistanceMatrix distanceMatrix = param
								.getParcelable(RequestFactory.BUNDLE_DISTANCE_MATRIX_DATA);
						OnDistanceMatrixReceived(distanceMatrix);
					}
				});
		m_stateMachine.AddTransition(EmState.FuelInfoReceived,
				EmState.Start, EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
                        StopTimer();
						RequestFuelInfo();
						Notify();
					}
				});

		m_stateMachine.AddTransition(EmState.FuelInfoReceived, EmState.Timeout,
				EmEvent.Timeout, new EventAction() {
					public void PerformAction(Bundle param) {
						m_timeoutEvent = EmEvent.DistanceEvent;
						Notify();
					}
				});
		m_stateMachine.AddTransition(EmState.DistanceReceived,
				EmState.Start, EmEvent.Refresh, new EventAction() {
					public void PerformAction(Bundle param) {
                        StopTimer();
						RequestFuelInfo();
						Notify();
					}
				});
	}

    private void OnFuelInfoReceived(List<FuelInfo> fuelInfoList, Date dayOfFuel, boolean isDataFromCache) {
        UpdateFuelInfoList(fuelInfoList);
        if (m_fuelInfoList.size() != 0) {
            RequestDistanceMatrix(m_fuelInfoList);
        } else {
            m_stateMachine.SetState(EmState.DistanceReceived);
        }
        if(!isDataFromCache){
            m_cacheManager.CacheFuelInfo(m_settings, m_suburb, dayOfFuel, m_fuelInfoList);
        }
        Notify();
    }

    private void UpdateFuelInfoList(List<FuelInfo> fuelInfoList) {
        if(m_fuelInfoList != fuelInfoList){
            m_fuelInfoList = fuelInfoList;
            m_fuelInfoChanged = true;
        }
    }

	private void OnDistanceMatrixReceived(DistanceMatrix distanceMatrix) {
		m_fuelDistanceItems = new ArrayList<FuelDistanceItem>();
        m_fuelDistanceInfoChanged = true;
		int i = 0;
		for (DistanceMatrixItem distanceItem : distanceMatrix
				.GetDistanceItems()) {
			FuelDistanceItem item = new FuelDistanceItem();
			item.distance = distanceItem.distance;
			item.distanceValue = distanceItem.distanceValue;
			item.duration = distanceItem.duration;
			FuelInfo fuelInfo = this.m_fuelInfoList.get(i);
			item.tradingName = fuelInfo.tradingName;
			item.price = fuelInfo.price;
			String lowTradingName = item.tradingName
					.toLowerCase(Locale.ENGLISH);
			if (lowTradingName.contains("woolworths")
					&& m_settings.m_wwsDiscount > 0) {
				item.price -= m_settings.m_wwsDiscount;
				item.voucher = m_settings.m_wwsDiscount;
				item.voucherType = "wws";
			} else if (lowTradingName.contains("coles")
					&& m_settings.m_colesDiscount > 0) {
				item.price -= m_settings.m_colesDiscount;
				item.voucher = m_settings.m_colesDiscount;
				item.voucherType = "coles";
			} else {
				item.voucherType = "";
			}
			item.latitude = fuelInfo.latitude;
			item.longitude = fuelInfo.longitude;
			item.destinationAddr = fuelInfo.GetAddress();
			m_fuelDistanceItems.add(item);
			i++;
        }
        m_cacheManager.CacheFuelDistanceInfo(m_settings, m_suburb, m_address, GetDate(m_dateOfFuel), m_fuelDistanceItems);

        Notify();
    }

    private Date GetDate(PriceDate date) {
        if (date == PriceDate.Tomorrow) {
            return TimeUtil.Add(new Date(), 1);
        } else if (date == PriceDate.Yesterday) {
            return TimeUtil.Add(new Date(), -1);
        }
        return new Date();
    }

    @Override
    public void onRequestFinished(Request request, Bundle resultData) {
		if (request.getRequestType() == RequestFactory.REQ_TYPE_DISTANCE_MATRIX) {
			this.m_stateMachine.HandleEvent(EmEvent.DistanceEvent, resultData);
            return;
		}

		if (request.getRequestType() == RequestFactory.REQ_TYPE_GET_CUR_SUBURB) {
			this.m_stateMachine.HandleEvent(EmEvent.SuburbEvent, resultData);
            return;
		}

		if (request.getRequestType() == RequestFactory.REQ_TYPE_FUEL) {
			this.m_stateMachine.HandleEvent(EmEvent.FuelInfoEvent, resultData);
            return;
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

	private void RequestFuelInfo() {
        if(this.m_paused){
            return;
        }
        List<FuelInfo> cachedFuel = m_cacheManager.HitFuelInfoCache(m_settings, m_suburb, GetDate(m_dateOfFuel));
        if(cachedFuel != null){
            m_stateMachine.SetState(EmState.FuelInfoReceived);
            OnFuelInfoReceived(cachedFuel, GetDate(m_dateOfFuel), true);
            return;
        }
		StartTimer(5000);
		Request req = RequestFactory.GetFuelInfoRequest(m_suburb,
                m_settings.IncludeSurroundings(), m_settings.GetFuelType(), this.m_dateOfFuel.toString());
		m_restReqManager.execute(req, this);
	}

	private void RequestDistanceMatrix(List<FuelInfo> fuelInfoList) {
        if(this.m_paused){
            return;
        }
        List<FuelDistanceItem> cached = m_cacheManager.HitFuelDistanceCache(m_settings, m_suburb, m_address, GetDate(m_dateOfFuel));
        if (cached != null){
            m_stateMachine.SetState(EmState.DistanceReceived);
            if (m_fuelDistanceItems != cached)
            {
                m_fuelDistanceItems = cached;
                m_fuelDistanceInfoChanged = true;
            }
            return;
        }

		DestinationList destinations = new DestinationList();
		for (FuelInfo item : fuelInfoList) {
			destinations.AddDestination(item.latitude, item.longitude);
		}
        String src = this.m_address.replace(' ', '+');
		StartTimer(5000);
		Request req = RequestFactory
				.GetDistanceMatrixRequest(src, destinations);
		m_restReqManager.execute(req, this);
    }

	public EmState GetCurState() {
		return this.m_stateMachine.GetState();
	}

	private void Notify() {
		if(m_lastState != m_stateMachine.GetState() ||
                m_fuelDistanceInfoChanged||
                m_fuelInfoChanged){
            m_fuelDistanceInfoChanged = false;
            m_fuelInfoChanged = false;
            m_lastState = m_stateMachine.GetState();
            setChanged();
            notifyObservers();
        }
	}

    private void Start() {
        m_stateMachine.SetState(EmState.Start);
        Notify();
        RequestFuelInfo();
    }

    public void RestoreFromSaveInstanceState(Bundle savedInstanceState){
        m_cacheManager.RestoreFromSaveInstanceState(savedInstanceState);
    }

    public void SaveInstanceState(Bundle outState){
        m_cacheManager.SaveInstanceState(outState);
    }

    public void SetDateOfFuel(PriceDate newDate) {
        m_dateOfFuel = newDate;
    }

    public void Pause() {
        this.m_paused = true;
        m_restReqManager.removeRequestListener(this);
        m_cacheManager.SaveToFile();
    }

    public boolean IsPaused(){
        return this.m_paused;
    }

    public void ClearFuelInfo() {
        if (m_fuelInfoList != null) {
            m_fuelInfoList.clear();
        }
    }
}
