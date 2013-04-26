package com.ezhang.pop.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Element implements Parcelable
{
	public String text;
	public String value;
	
	// Parcelable management
    private Element(Parcel in) {
    	text = in.readString();
    	value = in.readString();
    }
    
	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(text);
        dest.writeString(value);
	}
}