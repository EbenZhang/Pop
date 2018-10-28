package com.ezhang.pop.ui;

import android.os.Bundle;

import com.ezhang.pop.core.ICallable;
import com.ezhang.pop.model.FuelDistanceItem;
import com.ezhang.pop.settings.AppSettings;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

/**
 * Created by EbenZhang on 25/07/13.
 */
public class GMapFragment extends MapFragment {
    ICallable<Object, Object> m_onGMapReady;
    boolean m_isReady = false;
    AppSettings m_settings;

    public GMapFragment() {
    }

    public void Init(ICallable<Object, Object> onGMapReady, AppSettings settings) {
        m_onGMapReady = onGMapReady;
        m_settings = settings;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                if (map == null) {
                    int isEnabled = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(GMapFragment.this.getActivity());
                    if (isEnabled != ConnectionResult.SUCCESS) {
                        GooglePlayServicesUtil.getErrorDialog(isEnabled, GMapFragment.this.getActivity(), 0);
                        return;
                    }
                }
                m_isReady = true;
                map.setOnCameraMoveListener (GetCameraChangeListener(map));
                if (m_onGMapReady != null) {
                    m_onGMapReady.Call(null);
                }
            }
        });
    }

    public void UpdateModel(final ArrayList<FuelDistanceItem> fuelItems) {
        if (!m_isReady || !this.getUserVisibleHint()) {
            return;
        }
        this.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                map.clear();
                if (fuelItems.size() == 0) {
                    return;
                }
                FuelDistanceItem firstItem = fuelItems.get(0);
                LatLng latLng = new LatLng(Double.parseDouble(firstItem.latitude), Double.parseDouble(firstItem.longitude));
                InitMapCamera(map, latLng);
                UpdateView(map, fuelItems);
            }
        });
    }

    private void InitMapCamera(GoogleMap map, LatLng latLng) {
        CameraUpdate center =
                CameraUpdateFactory.newLatLng(latLng);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(m_settings.GetZoomLevel());
        map.moveCamera(center);
        map.animateCamera(zoom);
    }


    private static void UpdateView(GoogleMap map, ArrayList<FuelDistanceItem> fuelItems) {
        int index = 0;
        for (FuelDistanceItem i : fuelItems) {
            MarkerOptions markerOp = new MarkerOptions();
            markerOp.position(new LatLng(Double.parseDouble(i.latitude), Double.parseDouble(i.longitude)));
            markerOp.title(i.GetPriceString() + i.GetDistanceAndDurationString());
            markerOp.snippet(i.tradingName);
            markerOp.icon(GetMarkerIcon(index, fuelItems.size()));
            Marker marker = map.addMarker(markerOp);
            if(index == 0){
                marker.showInfoWindow();
            }
            ++index;
        }
    }

    private static BitmapDescriptor GetMarkerIcon(int index, int total) {
        if ((float) index <= (float) total / 3.0f) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        } else if ((float) index > (float) total / 3.0f && (float) index <= (float) total * 2.0f / 3.0f) {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        } else {
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }
    }


    public void Clear() {
        this.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap map) {
                map.clear();
            }
        });
    }

    private GoogleMap.OnCameraMoveListener GetCameraChangeListener(final GoogleMap map)
    {
        return new GoogleMap.OnCameraMoveListener() {
            Boolean firstRun = true; // the OS will setup an initial zoom level, which shouldn't be recorded.
            @Override
            public void onCameraMove()
            {
                if(!firstRun){
                    CameraPosition position = map.getCameraPosition();
                    if(m_settings.GetZoomLevel() != (int)position.zoom)
                    {
                        m_settings.SetZoomLevel((int)position.zoom);
                    }
                }
                firstRun = false;
            }
        };
    }
}
