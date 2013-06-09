package com.ezhang.pop.network;

import android.location.Location;
import com.ezhang.pop.RequestOperations.CurrentAddressQueryOperation;
import com.ezhang.pop.RequestOperations.DistanceMatrixQueryOperation;
import com.ezhang.pop.RequestOperations.FuelInfoOperation;
import com.ezhang.pop.model.DestinationList;
import com.ezhang.pop.utils.LocationFormatter;
import com.foxykeep.datadroid.requestmanager.Request;

import java.text.SimpleDateFormat;
import java.util.Date;

public class RequestFactory {
	// Request types
    public static final int REQ_TYPE_DISTANCE_MATRIX = 1;
    
    public static final int REQ_TYPE_GET_CUR_SUBURB = 2;
    
    public static final int REQ_TYPE_FUEL = 3;
    
    // Response data
    public static final String BUNDLE_DISTANCE_MATRIX_DATA =
            "com.ezhang.pop.distanceMatrix";
    
    public static final String BUNDLE_CUR_SUBURB_DATA =
            "com.ezhang.pop.current.suburb";
    
    public static final String BUNDLE_CUR_ADDRESS_DATA =
            "com.ezhang.pop.current.address";

	public static final String BUNDLE_FUEL_DATA = "com.ezhang.pop.fuel.info";
    public static final String BUNDLE_FUEL_INFO_PUBLISH_DATE = "com.ezhang.pop.fuel.info.publish.date";
	
	public static final String BUNDLE_LOCATION_DATA = "com.ezhang.pop.location.data";
    
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
        request.put(CurrentAddressQueryOperation.CUR_GEO_LOCATION, LocationFormatter.GoogleQueryFormat(location));
        return request;
    }

	public static Request GetFuelInfoRequest(String m_suburb, boolean includeSurrounding, int fuelType, Date dateOfFuel) {
		Request request = new Request(REQ_TYPE_FUEL);
        request.setMemoryCacheEnabled(true);
        request.put(FuelInfoOperation.SUBURB, m_suburb);
        request.put(FuelInfoOperation.INCLUDE_SURROUNDING, includeSurrounding);
        request.put(FuelInfoOperation.FUEL_TYPE, fuelType);
        SimpleDateFormat f = new SimpleDateFormat("dd/MM/yyyy");
        String date = f.format(dateOfFuel);
        request.put(FuelInfoOperation.FUEL_DATE, date);
        return request;
	}
}
