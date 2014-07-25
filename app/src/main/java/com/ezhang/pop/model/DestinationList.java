package com.ezhang.pop.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class DestinationList implements Parcelable {
	private List<Destination> m_destinations = new ArrayList<Destination>();
	private DestinationList(Parcel in) {
    	in.readList(m_destinations, Destination.class.getClassLoader());
    }
	
	public DestinationList() {
	}

	public void AddDestination(String latitude, String longitude) {
		this.m_destinations.add(new Destination(latitude, longitude));
	}
	
	public final List<Destination> GetDestinations()
	{
		return this.m_destinations;
	}
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(m_destinations);
	}

	public static final Parcelable.Creator<DestinationList> CREATOR = new Parcelable.Creator<DestinationList>() {
		public DestinationList createFromParcel(Parcel in) {
			return new DestinationList(in);
		}

		public DestinationList[] newArray(int size) {
			return new DestinationList[size];
		}
	};
}
