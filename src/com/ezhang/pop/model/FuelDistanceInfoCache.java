package com.ezhang.pop.model;

import com.ezhang.pop.core.TimeUtil;
import com.ezhang.pop.settings.AppSettings;

import java.util.List;

/**
 * Created by eben on 13-5-20.
 */
public class FuelDistanceInfoCache {
    public List<FuelDistanceItem> m_cachedFuelDistanceInfo;
    final FuelCacheParam fuelCacheParam = new FuelCacheParam();
    String m_address = null;

    public void SetCacheContext(AppSettings settings, String suburb, String address) {
        fuelCacheParam.m_cachedFuelType = settings.GetFuelType();
        fuelCacheParam.m_cachedIncludeSurroundings = settings.IncludeSurroundings();
        fuelCacheParam.m_colesVoucher = settings.m_colesDiscount;
        fuelCacheParam.m_wwsVoucher = settings.m_wwsDiscount;
        fuelCacheParam.m_cachedSuburb = suburb;
        m_cachedFuelDistanceInfo = null;
        m_address = address;
    }

    public void CacheFuelInfo(List<FuelDistanceItem> fuelDistanceItems) {
        m_cachedFuelDistanceInfo = fuelDistanceItems;
        fuelCacheParam.m_cachedDay = TimeUtil.GetCurDay();
    }

    public boolean HitCache(AppSettings settings, String suburb, String address) {
        boolean hit = fuelCacheParam.HitCache(settings, suburb);
        hit &= m_cachedFuelDistanceInfo != null && m_cachedFuelDistanceInfo.size() != 0;
        hit &= m_address == address;
        return hit;
    }
}
