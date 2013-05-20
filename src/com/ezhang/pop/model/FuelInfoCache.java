package com.ezhang.pop.model;

import com.ezhang.pop.core.TimeUtil;
import com.ezhang.pop.settings.AppSettings;

import java.util.List;

/**
 * Created by eben on 13-5-20.
 */
public class FuelInfoCache {
    final FuelCacheParam fuelCacheParam = new FuelCacheParam();
    public List<FuelInfo> m_cachedFuelInfo;

    public void SetCacheContext(AppSettings settings, String suburb) {
        fuelCacheParam.m_cachedFuelType = settings.GetFuelType();
        fuelCacheParam.m_cachedIncludeSurroundings = settings.IncludeSurroundings();
        fuelCacheParam.m_colesVoucher = settings.m_colesDiscount;
        fuelCacheParam.m_wwsVoucher = settings.m_wwsDiscount;
        fuelCacheParam.m_cachedSuburb = suburb;
        m_cachedFuelInfo = null;
    }

    public void CacheFuelInfo(List<FuelInfo> fuelInfo) {
        m_cachedFuelInfo = fuelInfo;
        fuelCacheParam.m_cachedDay = TimeUtil.GetCurDay();
    }

    public boolean HitCache(AppSettings settings, String suburb) {
        boolean hit = fuelCacheParam.HitCache(settings, suburb);
        hit &= m_cachedFuelInfo != null && m_cachedFuelInfo.size() != 0;
        return hit;
    }
}
