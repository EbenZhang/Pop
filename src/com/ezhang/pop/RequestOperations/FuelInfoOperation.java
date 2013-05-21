package com.ezhang.pop.RequestOperations;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import android.content.Context;
import android.os.Bundle;

import com.ezhang.pop.model.FuelInfoXmlHandler;
import com.ezhang.pop.network.RequestFactory;
import com.foxykeep.datadroid.exception.ConnectionException;
import com.foxykeep.datadroid.exception.DataException;
import com.foxykeep.datadroid.network.NetworkConnection;
import com.foxykeep.datadroid.network.NetworkConnection.ConnectionResult;
import com.foxykeep.datadroid.network.NetworkConnection.Method;
import com.foxykeep.datadroid.requestmanager.Request;
import com.foxykeep.datadroid.service.RequestService.Operation;

public class FuelInfoOperation implements Operation {
	public static final String SUBURB = "com.ezhang.pop.current.suburb";
	public static final String INCLUDE_SURROUNDING = "com.ezhang.pop.include.surrounding";
	public static final String FUEL_TYPE = "com.ezhang.pop.fueltype";

	@Override
	public Bundle execute(Context context, Request request)
			throws ConnectionException, DataException {
		String suburb = request.getString(SUBURB).replace(' ', '+');
		boolean includeSurrounding = request.getBoolean(INCLUDE_SURROUNDING);
		String includeSurroundingString = includeSurrounding? "yes":"no";
		int fuelType = request.getInt(FUEL_TYPE);

		String qry = String
				.format("http://www.fuelwatch.wa.gov.au/fuelwatch/fuelWatchRSS?Suburb=%s&Surrounding=%s&Product=%d",
						suburb, includeSurroundingString, fuelType);

		NetworkConnection networkConnection = new NetworkConnection(context,
				qry);
		networkConnection.setMethod(Method.GET);
		ConnectionResult result = networkConnection.execute();

		return parseResult(result.body);
	}

	private Bundle parseResult(String response) {
		SAXParserFactory spf = SAXParserFactory.newInstance();
		FuelInfoXmlHandler handler = new FuelInfoXmlHandler();
		try {
			SAXParser saxParser = spf.newSAXParser();

			saxParser.parse(
					new ByteArrayInputStream(response.getBytes("UTF-8")),
					handler);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		Bundle bundle = new Bundle();
		bundle.putParcelableArrayList(RequestFactory.BUNDLE_FUEL_DATA,
				handler.FuelItems);
		return bundle;
	}
}