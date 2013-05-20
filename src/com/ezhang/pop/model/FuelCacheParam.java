package com.ezhang.pop.model;

import com.ezhang.pop.core.TimeUtil;
import com.ezhang.pop.settings.AppSettings;

public class FuelCacheParam {
    int m_cachedFuelType = 0;
    int m_cachedDay = 0;
    int m_wwsVoucher = 0;
    int m_colesVoucher = 0;
    boolean m_cachedIncludeSurroundings = false;
    String m_cachedSuburb = null;

    public FuelCacheParam() {
    }

    public boolean HitCache(AppSettings settings, String suburb)
    {
        boolean hit = m_cachedDay == TimeUtil.GetCurDay();
        hit &= m_cachedFuelType == settings.GetFuelType();
        hit &= settings.m_wwsDiscount == m_wwsVoucher;
        hit &= settings.m_colesDiscount == m_colesVoucher;
        hit &= suburb.equals(m_cachedSuburb);
        return hit;
    }
}