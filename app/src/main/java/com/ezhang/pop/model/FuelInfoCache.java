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
public class FuelInfoCache implements Parcelable {
    private FuelCacheParam m_fuelCacheParam = new FuelCacheParam();
    public List<FuelInfo> m_cachedFuelInfo = new ArrayList<FuelInfo>();

    public FuelInfoCache(){
    }
    public FuelInfoCache(Parcel in){
        m_fuelCacheParam = in.readParcelable(FuelCacheParam.class.getClassLoader());
        m_cachedFuelInfo = new ArrayList<FuelInfo>();
        in.readList(m_cachedFuelInfo, FuelInfo.class.getClassLoader());
    }

    private void SetCacheContext(AppSettings settings, String suburb, Date dateOfFuel) {
        m_fuelCacheParam.m_cachedFuelType = settings.GetFuelType();
        m_fuelCacheParam.m_cachedIncludeSurroundings = settings.IncludeSurroundings();
        m_fuelCacheParam.m_colesVoucher = settings.m_colesDiscount;
        m_fuelCacheParam.m_wwsVoucher = settings.m_wwsDiscount;
        m_fuelCacheParam.m_cachedSuburb = suburb;
        m_fuelCacheParam.m_cachedDay = TimeUtil.GetDayFromDate(dateOfFuel);
    }

    public void CacheFuelInfo(AppSettings settings, String suburb, Date dayOfFuel, List<FuelInfo> fuelInfo) {
        if (m_cachedFuelInfo != fuelInfo) {
            m_cachedFuelInfo.clear();
            m_cachedFuelInfo.addAll(fuelInfo);
        }
        SetCacheContext(settings, suburb, dayOfFuel);
    }

    public boolean HitCache(AppSettings settings, String suburb, Date dayOfFuel) {
        boolean hit = m_fuelCacheParam.HitCache(settings, suburb, dayOfFuel);
        hit &= m_cachedFuelInfo != null && m_cachedFuelInfo.size() != 0;
        return hit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(m_fuelCacheParam, i);
        if(m_cachedFuelInfo == null){
            parcel.writeList(new ArrayList<FuelInfo>());
        }else{
            parcel.writeList(m_cachedFuelInfo);
        }
    }

    public static final Parcelable.Creator<FuelInfoCache> CREATOR = new Parcelable.Creator<FuelInfoCache>() {
        public FuelInfoCache createFromParcel(Parcel in) {
            return new FuelInfoCache(in);
        }

        public FuelInfoCache[] newArray(int size) {
            return new FuelInfoCache[size];
        }
    };
}
