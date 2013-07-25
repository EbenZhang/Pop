package com.ezhang.pop.ui;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.location.Address;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ezhang.pop.R;
import com.ezhang.pop.core.ICallable;
import com.ezhang.pop.model.FuelDistanceItem;
import com.foxykeep.datadroid.network.NetworkConnection;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by EbenZhang on 25/07/13.
 */
public class GMapFragment extends SupportMapFragment {
    ICallable<Object, Object> m_onGMapReady;
    public GMapFragment(ICallable<Object, Object> onGMapReady){
        m_onGMapReady = onGMapReady;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        GoogleMap map = getMap();
        if(map == null){
            int isEnabled = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getActivity());
            if(isEnabled != ConnectionResult.SUCCESS){
                GooglePlayServicesUtil.getErrorDialog(isEnabled, this.getActivity(), 0);
                return;
            }
        }
        getMap().setMyLocationEnabled(true);
        m_onGMapReady.Call(null);
    }

    public void UpdateModel(ArrayList<FuelDistanceItem> fuelItems) {
        if (this.getUserVisibleHint()) {
            GoogleMap map = this.getMap();
            if(map == null){
                return;
            }
            map.clear();
            if (fuelItems.size() == 0){
                return;
            }
            FuelDistanceItem firstItem = fuelItems.get(0);
            LatLng latLng = new LatLng(Double.parseDouble(firstItem.latitude), Double.parseDouble(firstItem.longitude));
            InitMapCamera(map, latLng);
            UpdateView(map, fuelItems);
        }
    }

    private static void InitMapCamera(GoogleMap map, LatLng latLng) {
        CameraUpdate center =
                CameraUpdateFactory.newLatLng(latLng);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(13);
        map.moveCamera(center);
        map.animateCamera(zoom);
    }


    private static void UpdateView(GoogleMap map, ArrayList<FuelDistanceItem> fuelItems) {
        int index = 0;
        for(FuelDistanceItem i : fuelItems) {
            MarkerOptions markerOp = new MarkerOptions();
            markerOp.position(new LatLng(Double.parseDouble(i.latitude), Double.parseDouble(i.longitude)));
            markerOp.title(i.GetPriceString() + i.GetDistanceAndDurationString());
            markerOp.snippet(i.tradingName);
            markerOp.icon(GetMarkerIcon(index, fuelItems.size()));
            map.addMarker(markerOp);
            ++index;
        }
    }
    private static BitmapDescriptor GetMarkerIcon(int index, int total){
        if((float)index <= (float)total/3.0f){
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN);
        }else if((float)index > (float)total/3.0f && (float)index <= (float)total*2.0f/3.0f){
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW);
        }else{
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED);
        }
    }


    public void Clear() {
        GoogleMap map = this.getMap();
        if(map != null){
            map.clear();
        }
    }
}
