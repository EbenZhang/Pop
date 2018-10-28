package com.ezhang.pop.ui;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.ezhang.pop.R;
import com.ezhang.pop.core.ICallable;
import com.ezhang.pop.model.FuelDistanceItem;

import java.util.ArrayList;

/**
 * Created by EbenZhang on 25/07/13.
 */
public class ListViewFragment extends Fragment {
    private ListView m_fuelDistanceItemlistView = null;
    private ArrayList<FuelDistanceItem> m_fuelInfoList;
    ICallable<Object, Object> m_onFragmentCreated;
    boolean m_isReady = false;
    public ListViewFragment(){
        m_fuelInfoList = new ArrayList<FuelDistanceItem>();
    }
    public void Init(ICallable<Object, Object> onFragmentCreated, ArrayList<FuelDistanceItem> fuelInfoList){
        m_onFragmentCreated = onFragmentCreated;
        m_fuelInfoList = fuelInfoList;
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.listview_fragment, container, false);

        return v;
    }
    @Override
    public void onActivityCreated(Bundle savedInstanceBundle){
        super.onActivityCreated(savedInstanceBundle);
        m_fuelDistanceItemlistView = (ListView) this.getView().findViewById(R.id.fuelDistanceItemlistView);
        m_fuelDistanceItemlistView.setAdapter(new FuelListAdapter(getActivity(), m_fuelInfoList));
        m_fuelDistanceItemlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> a, View v, int position,
                                    long id) {
                Object o = m_fuelDistanceItemlistView.getItemAtPosition(position);
                FuelDistanceItem fullObject = (FuelDistanceItem) o;
                LaunchNavigationApp(getActivity(), fullObject.latitude, fullObject.longitude);
            }
        });

        m_isReady = true;
        if(m_onFragmentCreated != null){
            m_onFragmentCreated.Call(null);
        }
    }
    private void LaunchNavigationApp(Context ctx, String dstLatitude, String dstLongitude) {
        String uriString = String.format("geo:0,0?q=%s,%s", dstLatitude,
                dstLongitude);
        Uri uri = Uri.parse(uriString);
        Intent intent = new Intent(android.content.Intent.ACTION_VIEW, uri);
        ctx.startActivity(intent);
    }

    public void UpdateViewForModel() {
        if (!m_isReady || !this.getUserVisibleHint()){
            return;
        }
        ((BaseAdapter) m_fuelDistanceItemlistView.getAdapter()).notifyDataSetChanged();
        this.m_fuelDistanceItemlistView.smoothScrollToPosition(0);
    }
}
