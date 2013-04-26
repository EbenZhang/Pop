package com.ezhang.pop.model;



import android.os.Parcel;
import android.os.Parcelable;

public class FuelInfo implements Parcelable {
	public float price;
	public String brand;
	public String address;
	public String latitude;
	public String longitude;
	public String tradingName;
	
	private FuelInfo(Parcel in) {
		price = in.readFloat();
		brand= in.readString();
		address = in.readString();
		latitude = in.readString();
		longitude = in.readString();
		tradingName = in.readString();
    }
	
	public FuelInfo() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeFloat(price);
		dest.writeString(brand);
		dest.writeString(address);
		dest.writeString(latitude);
		dest.writeString(longitude);
		dest.writeString(tradingName);
	}
	
	public static final Parcelable.Creator<FuelInfo> CREATOR = new Parcelable.Creator<FuelInfo>() {
		public FuelInfo createFromParcel(Parcel in) {
			return new FuelInfo(in);
		}

		public FuelInfo[] newArray(int size) {
			return new FuelInfo[size];
		}
	};
}
