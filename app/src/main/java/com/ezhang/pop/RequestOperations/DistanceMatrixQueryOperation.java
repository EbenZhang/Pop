package com.ezhang.pop.RequestOperations;

import android.content.Context;
import android.os.Bundle;

import com.ezhang.pop.R;
import com.ezhang.pop.model.Destination;
import com.ezhang.pop.model.DestinationList;
import com.ezhang.pop.model.DistanceMatrix;
import com.ezhang.pop.network.RequestFactory;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;
import com.foxykeep.datadroid.network.NetworkConnection;
import com.foxykeep.datadroid.network.NetworkConnection.ConnectionResult;
import com.foxykeep.datadroid.network.NetworkConnection.Method;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.service.RequestService.Operation;
import com.google.gson.Gson;

public class DistanceMatrixQueryOperation implements Operation {
	public static final String ORIGIN = "com.ezhang.pop.distancematrix.origin";
	public static final String DESTINATION = "com.ezhang.pop.distancematrix.destination";

	@Override
	public Bundle execute(Context context, Request request)
			throws ConnectionException, DataException {
		String org = request.getString(ORIGIN);
		DestinationList dstList = (DestinationList) request
				.getParcelable(DESTINATION);

		String apiKey = context.getResources().getString(R.string.google_maps_key);
		String qry = make_distance_query(org, dstList, apiKey);

		NetworkConnection networkConnection = new NetworkConnection(context,
				qry);
		networkConnection.setMethod(Method.GET);
		ConnectionResult result = networkConnection.execute();

		return parseResult(result.body);
	}

	private Bundle parseResult(String response) {
		Gson gson = new Gson();
		DistanceMatrix distanceMatrix = gson.fromJson(response,
				DistanceMatrix.class);

		Bundle bundle = new Bundle();
		bundle.putParcelable(RequestFactory.BUNDLE_DISTANCE_MATRIX_DATA,
				distanceMatrix);
		return bundle;
	}

	private String make_distance_query(String src, DestinationList dstList, String apiKey) {
		StringBuilder dstBuilder = new StringBuilder();

		for (Destination dst : dstList.GetDestinations()) {
			dstBuilder.append(dst.latitude + "," + dst.longitude);
			dstBuilder.append("|");
		}
		dstBuilder.deleteCharAt(dstBuilder.length() - 1);

		String header = "https://maps.googleapis.com/maps/api/distancematrix/json?key=" + apiKey;
		return header + "&origins=" + src + "&destinations="
				+ dstBuilder.toString() + "&sensor=false";
	}
}
