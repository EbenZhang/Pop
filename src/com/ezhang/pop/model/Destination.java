package com.ezhang.pop.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Destination implements Parcelable
{
	public String latitude;
	public String longitude;
	private Destination(Parcel in) {
		latitude = in.readString();
		longitude = in.readString();
    }
	public Destination(String _latitude, String _longitude) {
		latitude = _latitude;
		longitude = _longitude;
    }
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(this.latitude);
		dest.writeString(this.longitude);
	}
	public static final Parcelable.Creator<Destination> CREATOR = new Parcelable.Creator<Destination>() {
		public Destination createFromParcel(Parcel in) {
			return new Destination(in);
		}

		public Destination[] newArray(int size) {
			return new Destination[size];
		}
	};
}