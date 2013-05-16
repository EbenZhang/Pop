package com.ezhang.pop.core;

import android.util.Pair;

public class LocationSpliter {
	private static final String SEP = ", ";
	/**
	 * @return Street Address and Suburb
	 * */
	public static Pair<String,String> Split(String location) {
		String[] tokens = location.split(SEP);
		String suburb = tokens[tokens.length - 2]; // -2 for Western Australia
		String streetAddr = location.substring(0, location.indexOf(suburb) - SEP.length()).trim();
		return Pair.create(streetAddr, suburb);
	}

	public static String Combine(String streetAddr, String suburb) {
		return streetAddr + SEP + suburb + SEP + "Western Australia";
	}
}
