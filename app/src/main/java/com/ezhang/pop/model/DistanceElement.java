package com.ezhang.pop.model;

import android.os.Parcel;
import android.os.Parcelable;

class DistanceElement implements Parcelable
{
	public Element distance;
	public Element duration;
	public String status;
	
	// Parcelable management
    private DistanceElement(Parcel in) {
    	distance = in.readParcelable(Element.class.getClassLoader());
    	duration = in.readParcelable(Element.class.getClassLoader());
    	status = in.readString();
    }
    
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		distance.writeToParcel(dest, flags);
		duration.writeToParcel(dest, flags);
		dest.writeString(status);
	}
}