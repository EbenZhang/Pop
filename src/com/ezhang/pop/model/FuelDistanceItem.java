package com.ezhang.pop.model;

import java.util.Comparator;

import android.os.Parcel;
import android.os.Parcelable;

public class FuelDistanceItem extends DistanceMatrixItem implements Parcelable {
	public Float price;
	public String tradingName;
	public String latitude;
	public String longitude;
	
	public FuelDistanceItem()
	{
		
	}
	public FuelDistanceItem(Parcel in)
	{
		super.destinationAddr = in.readString();
		super.distance = in.readString();
		super.duration = in.readString();
		this.price = in.readFloat();
		this.tradingName = in.readString();
		this.latitude = in.readString();
		this.longitude = in.readString();
	}

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

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int arg1) {
		dest.writeString(super.destinationAddr);
		dest.writeString(super.distance);
		dest.writeString(super.duration);
		dest.writeFloat(this.price);
		dest.writeString(this.tradingName);
		dest.writeString(this.latitude);
		dest.writeString(this.longitude);
	}
	
	public static final Parcelable.Creator<FuelDistanceItem> CREATOR = new Parcelable.Creator<FuelDistanceItem>() {
		public FuelDistanceItem createFromParcel(Parcel in) {
			return new FuelDistanceItem(in);
		}

		public FuelDistanceItem[] newArray(int size) {
			return new FuelDistanceItem[size];
		}
	};
}
