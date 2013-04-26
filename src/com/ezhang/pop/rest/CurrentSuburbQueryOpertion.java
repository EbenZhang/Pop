package com.ezhang.pop.rest;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.os.Bundle;

import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;
import com.foxykeep.datadroid.network.NetworkConnection;
import com.foxykeep.datadroid.network.NetworkConnection.ConnectionResult;
import com.foxykeep.datadroid.network.NetworkConnection.Method;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.service.RequestService.Operation;

public class CurrentSuburbQueryOpertion implements Operation {
	
	public static final String CUR_GEO_LOCATION = "com.ezhang.pop.current.location";

	@Override
	public Bundle execute(Context context, Request request)
			throws ConnectionException, DataException {
		String curGeoLocation = request.getString(CUR_GEO_LOCATION);

		String qry = String
				.format("http://maps.googleapis.com/maps/api/geocode/json?latlng=%s&sensor=false",
						curGeoLocation);

		NetworkConnection networkConnection = new NetworkConnection(context,
				qry);
		networkConnection.setMethod(Method.GET);
		ConnectionResult result = networkConnection.execute();

		return parseResult(result.body);
	}

	private Bundle parseResult(String response) {
		String suburb = "";
		try {
			int beginOfFirstAddrComponent = response.indexOf("\"address_components\"");
			int endOfFirstAddrComponent = response.indexOf("\"formatted_address\"");
			response = "{" + response.substring(beginOfFirstAddrComponent, endOfFirstAddrComponent) + "\"dummy\":\"dummy\"}";
			JSONObject obj = new JSONObject(response);
			JSONArray addrComponents = obj.getJSONArray("address_components");
			for(int i =0; i< addrComponents.length();++i)
			{
				JSONObject addrComponent = addrComponents.getJSONObject(i);
				JSONArray types = addrComponent.getJSONArray("types");
				for(int j = 0; j <types.length();++j)
				{					
					String type = types.getString(j);
					if(type.equals("locality"))
					{
						suburb = addrComponent.getString("long_name");
						break;
					}
				}
				if(suburb != "")
				{
					break;
				}
			}
		} catch (Exception ex) {
		}
		Bundle bundle = new Bundle();
		bundle.putString(PopRequestFactory.BUNDLE_CUR_SUBURB_DATA, suburb);
		return bundle;
	}
}