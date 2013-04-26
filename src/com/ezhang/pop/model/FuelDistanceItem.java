package com.ezhang.pop.model;

import java.util.Comparator;

public class FuelDistanceItem extends DistanceMatrixItem {
	public Float price;
	public String tradingName;
	public String latitude;
	public String longitude;

	public static Comparator<FuelDistanceItem> GetComparer() {
		return new FuelDistanceComparer();
	}

	private static class FuelDistanceComparer implements
			Comparator<FuelDistanceItem> {
		@Override
		public int compare(FuelDistanceItem lhs, FuelDistanceItem rhs) {
			return lhs.price.compareTo(rhs.price);
		}
	}
}
