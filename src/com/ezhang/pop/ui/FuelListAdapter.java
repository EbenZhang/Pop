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
			holder.price = (TextView) convertView
					.findViewById(R.id.price);
            holder.distanceAndDuration = (TextView) convertView
                    .findViewById(R.id.distanceAndDuration);
			holder.tradingName = (TextView) convertView
					.findViewById(R.id.tradingName);
			holder.address = (TextView) convertView.findViewById(R.id.address);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		FuelDistanceItem item = fuelDistaceList.get(position);

		String price;
		if (item.voucherType != null && !item.voucherType.equals("")) {
			price = String.format(Locale.ENGLISH,
					"%.1f (%s -%dC)", item.price, item.voucherType,
					item.voucher);
		} else {
			price = String.format(Locale.ENGLISH, "%.1f",
					item.price);
		}
		holder.price.setText(price);
        holder.distanceAndDuration.setText(String.format(Locale.ENGLISH, " %s(%s)", item.distance, item.duration));
		holder.tradingName.setText(item.tradingName);
		holder.address.setText(item.destinationAddr);

		return convertView;
	}

	static class ViewHolder {
		TextView price;
        TextView distanceAndDuration;
		TextView tradingName;
		TextView address;
	}
}
