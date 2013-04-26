package com.ezhang.pop.rest;

import android.location.Location;

import com.ezhang.pop.model.DestinationList;
import com.ezhang.pop.utils.LocationFormatter;
import com.foxykeep.datadroid.requestmanager.Request;

public class PopRequestFactory {
	// Request types
    public static final int REQ_TYPE_DISTANCE_MATRIX = 1;
    
    public static final int REQ_TYPE_GET_CUR_SUBURB = 2;
    
    public static final int REQ_TYPE_FUEL = 3;
    
    // Response data
    public static final String BUNDLE_DISTANCE_MATRIX_DATA =
            "com.ezhang.pop.distanceMatrix";
    
    public static final String BUNDLE_CUR_SUBURB_DATA =
            "com.ezhang.pop.current.address";

	public static final String BUNDLE_FUEL_DATA = "com.ezhang.pop.fuel.info";
    
    public static Request GetDistanceMatrixRequest(String src, DestinationList dstList) {
        Request request = new Request(REQ_TYPE_DISTANCE_MATRIX);
        request.setMemoryCacheEnabled(true);
        request.put(DistanceMatrixQueryOperation.ORIGIN, src);
        request.put(DistanceMatrixQueryOperation.DESTINATION, dstList);
        return request;
    }
    
    public static Request GetCurrentSuburbRequest(Location location) {
        Request request = new Request(REQ_TYPE_GET_CUR_SUBURB);
        request.setMemoryCacheEnabled(true);
        request.put(CurrentSuburbQueryOpertion.CUR_GEO_LOCATION, LocationFormatter.GoogleQueryFormat(location));
        return request;
    }

	public static Request GetFuelInfoRequest(String m_suburb) {
		Request request = new Request(REQ_TYPE_FUEL);
        request.setMemoryCacheEnabled(true);
        request.put(FuelInfoOperation.SUBURB, m_suburb);
        return request;
	}
}
