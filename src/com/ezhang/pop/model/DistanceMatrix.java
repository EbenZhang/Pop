package com.ezhang.pop.model;

import java.util.ArrayList;
import java.util.List;

import android.os.Parcel;
import android.os.Parcelable;

public class DistanceMatrix implements Parcelable {
	public List<String> destination_addresses;
	public List<String> origin_addresses;
	public List<DistanceRow> rows;
	public String status;

	public final List<DistanceMatrixItem> GetDistanceItems() {
		List<DistanceMatrixItem> items = new ArrayList<DistanceMatrixItem>();
		for (int i = 0; i < this.destination_addresses.size(); ++i) {
			DistanceMatrixItem item = new DistanceMatrixItem();
			item.destinationAddr = this.destination_addresses.get(i);
			item.distance = this.rows.get(0).elements.get(i).distance.text;
			item.duration = this.rows.get(0).elements.get(i).duration.text;
			items.add(item);
		}
		return items;
	}

	// Parcelable management
	private DistanceMatrix(Parcel in) {
		in.readList(destination_addresses, String.class.getClassLoader());
		in.readList(origin_addresses, String.class.getClassLoader());
		in.readList(rows, DistanceElement.class.getClassLoader());
		status = in.readString();
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeList(destination_addresses);
		dest.writeList(origin_addresses);
		dest.writeList(rows);
		dest.writeString(status);
	}
}