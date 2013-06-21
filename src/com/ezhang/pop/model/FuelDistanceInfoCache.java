package com.ezhang.pop.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.ezhang.pop.core.TimeUtil;
import com.ezhang.pop.settings.AppSettings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by eben on 13-5-20.
 */
public class FuelDistanceInfoCache implements Parcelable {
    public List<FuelDistanceItem> m_cachedFuelDistanceInfo;
    private FuelCacheParam m_fuelCacheParam = new FuelCacheParam();
    private String m_address = null;

    public FuelDistanceInfoCache(){
    }
    public FuelDistanceInfoCache(Parcel in){
        m_fuelCacheParam = in.readParcelable(FuelCacheParam.class.getClassLoader());
        m_cachedFuelDistanceInfo = new ArrayList<FuelDistanceItem>();
        in.readList(m_cachedFuelDistanceInfo, FuelDistanceItem.class.getClassLoader());
        m_address = in.readString();
    }
    private void SetCacheContext(AppSettings settings, String suburb, String address, Date dayOfFuel) {
        m_fuelCacheParam.m_cachedFuelType = settings.GetFuelType();
        m_fuelCacheParam.m_cachedIncludeSurroundings = settings.IncludeSurroundings();
        m_fuelCacheParam.m_colesVoucher = settings.m_colesDiscount;
        m_fuelCacheParam.m_wwsVoucher = settings.m_wwsDiscount;
        m_fuelCacheParam.m_cachedSuburb = suburb;
        m_fuelCacheParam.m_cachedDay = TimeUtil.GetDayFromDate(dayOfFuel);
        m_address = address;
    }

    public void CacheFuelInfo(AppSettings settings, String suburb, String address, Date dayOfFuel, List<FuelDistanceItem> fuelDistanceItems) {
        m_cachedFuelDistanceInfo = fuelDistanceItems;
        SetCacheContext(settings, suburb, address, dayOfFuel);
    }

    public boolean HitCache(AppSettings settings, String suburb, String address, Date dayOfFuel) {
            boolean hit = m_fuelCacheParam.HitCache(settings, suburb, dayOfFuel);
            hit &= m_cachedFuelDistanceInfo != null && m_cachedFuelDistanceInfo.size() != 0;
            hit &= m_address != null && address.equalsIgnoreCase(m_address);
            return hit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(m_fuelCacheParam, i);
        if(m_cachedFuelDistanceInfo == null){
            parcel.writeList(new ArrayList<FuelDistanceItem>());
        }else{
            parcel.writeList(m_cachedFuelDistanceInfo);
        }
    }

    public static final Parcelable.Creator<FuelDistanceInfoCache> CREATOR = new Parcelable.Creator<FuelDistanceInfoCache>() {
        public FuelDistanceInfoCache createFromParcel(Parcel in) {
            return new FuelDistanceInfoCache(in);
        }

        public FuelDistanceInfoCache[] newArray(int size) {
            return new FuelDistanceInfoCache[size];
        }
    };
}
