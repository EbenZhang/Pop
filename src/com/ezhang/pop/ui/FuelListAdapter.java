package com.ezhang.pop.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.ezhang.pop.R;
import com.ezhang.pop.model.FuelDistanceItem;

import java.util.ArrayList;
import java.util.Locale;

class FuelListAdapter extends BaseAdapter {
	private static ArrayList<FuelDistanceItem> fuelDistaceList;
	private LayoutInflater mInflater;

	public FuelListAdapter(Context context, ArrayList<FuelDistanceItem> results) {
		fuelDistaceList = results;
		mInflater = LayoutInflater.from(context);
	}

	public int getCount() {
		return fuelDistaceList.size();
	}

	public Object getItem(int position) {
		return fuelDistaceList.get(position);
	}

	public long getItemId(int position) {
		return position;
	}

	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		if (convertView == null) {
			convertView = mInflater.inflate(R.layout.custom_row_view, null);
			holder = new ViewHolder();
			holder.priceAndDistance = (TextView) convertView
					.findViewById(R.id.priceAndDistance);
			holder.tradingName = (TextView) convertView
					.findViewById(R.id.tradingName);
			holder.address = (TextView) convertView.findViewById(R.id.address);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		FuelDistanceItem item = fuelDistaceList.get(position);

		String priceAndDistance;
		if (item.voucherType != null && item.voucherType != "") {
			priceAndDistance = String.format(Locale.ENGLISH,
					"%.1f (%s -%dC) %s(%s)", item.price, item.voucherType,
					item.voucher, item.distance, item.duration);
		} else {
			priceAndDistance = String.format(Locale.ENGLISH, "%.1f %s(%s)",
					item.price, item.distance, item.duration);
		}
		holder.priceAndDistance.setText(priceAndDistance);
		holder.tradingName.setText(item.tradingName);
		holder.address.setText(item.destinationAddr);

		return convertView;
	}

	static class ViewHolder {
		TextView priceAndDistance;
		TextView tradingName;
		TextView address;
	}
}
