package com.ezhang.pop.model;

import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class DistanceRow implements Parcelable{
	List<DistanceElement> elements;
	
	// Parcelable management
    private DistanceRow(Parcel in) {
    	in.readList(elements, DistanceElement.class.getClassLoader());
    }

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(elements);		
	}
}
