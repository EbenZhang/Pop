package com.ezhang.pop.model;

import android.os.Bundle;
import com.ezhang.pop.settings.AppSettings;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by eben on 13-6-9.
 */
public class CacheManager {
    static final int MAX_CACHE_SIZE = 10;
    /**
     * We will save the fuel distance info items and restore them when killed by the system (Such as rotation and switching between apps)
     * */
    private static final String SAVED_FUEL_INFO_ITEMS = "com.ezhang.pop.saved.fuel.info.items";

    /**
     * We will save the fuel distance info items and restore them when killed by the system (Such as rotation and switching between apps)
     * */
    private static final String SAVED_FUEL_DISTANCE_INFO_ITEMS = "com.ezhang.pop.saved.fuel.distance.info.items";

    ArrayList<FuelInfoCache> m_fuelInfoCaches = new ArrayList<FuelInfoCache>();
    ArrayList<FuelDistanceInfoCache> m_fuelDistanceCache = new ArrayList<FuelDistanceInfoCache>();

    public void CacheFuelInfo(AppSettings settings, String suburb, Date dayOfFuel, List<FuelInfo> fuelInfo) {
        if (this.HitFuelInfoCache(settings, suburb, dayOfFuel) == fuelInfo) {
            return;
        }
        FuelInfoCache cache = new FuelInfoCache();
        cache.CacheFuelInfo(settings, suburb, dayOfFuel, fuelInfo);
        m_fuelInfoCaches.add(0, cache);
        if(m_fuelInfoCaches.size() >= MAX_CACHE_SIZE)
        {
            m_fuelInfoCaches.remove(m_fuelInfoCaches.size() - 1);
        }
    }

    public List<FuelInfo> HitFuelInfoCache(AppSettings settings, String suburb, Date dayOfFuel) {
        for (FuelInfoCache info : m_fuelInfoCaches) {
            if (info.HitCache(settings, suburb, dayOfFuel)) {
                return info.m_cachedFuelInfo;
            }
        }
        return null;
    }

    public void CacheFuelDistanceInfo(AppSettings settings, String suburb, String address, Date dateOfFuel, List<FuelDistanceItem> fuelDistanceItems) {

        if (this.HitFuelDistanceCache(settings, suburb, address, dateOfFuel) == fuelDistanceItems) {
            return;
        }

        FuelDistanceInfoCache cache = new FuelDistanceInfoCache();
        cache.CacheFuelInfo(settings, suburb, address, dateOfFuel, fuelDistanceItems);
        m_fuelDistanceCache.add(0, cache);
        if(m_fuelDistanceCache.size() >= MAX_CACHE_SIZE)
        {
            m_fuelDistanceCache.remove(m_fuelDistanceCache.size() - 1);
        }
    }

    public List<FuelDistanceItem> HitFuelDistanceCache(AppSettings settings, String suburb, String address, Date dayOfFuel) {
        for (FuelDistanceInfoCache info : m_fuelDistanceCache) {
            if (info.HitCache(settings, suburb, address, dayOfFuel)) {
                return info.m_cachedFuelDistanceInfo;
            }
        }
        return null;
    }

    public void RestoreFromSaveInstanceState(Bundle savedInstanceState){
        List<FuelInfoCache> fuelInfoCaches = savedInstanceState.getParcelableArrayList(SAVED_FUEL_INFO_ITEMS);
        m_fuelInfoCaches.addAll(fuelInfoCaches);

        List<FuelDistanceInfoCache> fuelDistanceCaches = savedInstanceState.getParcelableArrayList(SAVED_FUEL_DISTANCE_INFO_ITEMS);
        m_fuelDistanceCache.addAll(fuelDistanceCaches);
    }
    public void SaveInstanceState(Bundle outState){
        outState.putParcelableArrayList(SAVED_FUEL_INFO_ITEMS, m_fuelInfoCaches);
        outState.putParcelableArrayList(SAVED_FUEL_DISTANCE_INFO_ITEMS, m_fuelDistanceCache);
    }
}
