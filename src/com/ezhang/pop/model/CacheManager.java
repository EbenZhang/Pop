package com.ezhang.pop.model;

import android.os.Bundle;
import com.ezhang.pop.settings.AppSettings;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
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

    ArrayList<FuelInfoCache> m_fuelInfoCaches;
    ArrayList<FuelDistanceInfoCache> m_fuelDistanceCache;

    private String m_externalFuelInfoFile;
    private String m_externalFuelDistanceInfoFile;

    public CacheManager(String externalCacheDir){
        m_externalFuelInfoFile = externalCacheDir + "/fuelInfoCache.json";
        m_externalFuelDistanceInfoFile = externalCacheDir + "/fuelDistanceInfoCache.json";
    }

    public void CacheFuelInfo(AppSettings settings, String suburb, Date dayOfFuel, List<FuelInfo> fuelInfo) {
        EnsureCacheExistence();
        FuelInfoCache existing = null;
        for (FuelInfoCache info : m_fuelInfoCaches) {
            if (info.HitCache(settings, suburb, dayOfFuel)) {
                existing = info;
                break;
            }
        }
        if (existing != null) {
            existing.CacheFuelInfo(settings, suburb, dayOfFuel, fuelInfo);
        } else {
            FuelInfoCache cache = new FuelInfoCache();
            cache.CacheFuelInfo(settings, suburb, dayOfFuel, fuelInfo);
            m_fuelInfoCaches.add(0, cache);
            if (m_fuelInfoCaches.size() >= MAX_CACHE_SIZE) {
                m_fuelInfoCaches.remove(m_fuelInfoCaches.size() - 1);
            }
        }
    }

    public List<FuelInfo> HitFuelInfoCache(AppSettings settings, String suburb, Date dayOfFuel) {
        EnsureCacheExistence();
        for (FuelInfoCache info : m_fuelInfoCaches) {
            if (info.HitCache(settings, suburb, dayOfFuel)) {
                return new ArrayList<FuelInfo>(info.m_cachedFuelInfo);
            }
        }
        return null;
    }

    public void CacheFuelDistanceInfo(AppSettings settings, String suburb, String address, Date dateOfFuel, List<FuelDistanceItem> fuelDistanceItems) {
        EnsureCacheExistence();
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
        EnsureCacheExistence();
        for (FuelDistanceInfoCache info : m_fuelDistanceCache) {
            if (info.HitCache(settings, suburb, address, dayOfFuel)) {
                return new ArrayList<FuelDistanceItem>(info.m_cachedFuelDistanceInfo);
            }
        }
        return null;
    }

    public void RestoreFromSaveInstanceState(Bundle savedInstanceState){
        EnsureCacheExistence();
        List<FuelInfoCache> fuelInfoCaches = savedInstanceState.getParcelableArrayList(SAVED_FUEL_INFO_ITEMS);
        m_fuelInfoCaches.addAll(fuelInfoCaches);

        List<FuelDistanceInfoCache> fuelDistanceCaches = savedInstanceState.getParcelableArrayList(SAVED_FUEL_DISTANCE_INFO_ITEMS);
        m_fuelDistanceCache.addAll(fuelDistanceCaches);
    }
    public void SaveInstanceState(Bundle outState){
        EnsureCacheExistence();
        outState.putParcelableArrayList(SAVED_FUEL_INFO_ITEMS, m_fuelInfoCaches);
        outState.putParcelableArrayList(SAVED_FUEL_DISTANCE_INFO_ITEMS, m_fuelDistanceCache);
        SaveToFile();
    }

    public void SaveToFile(){
        Gson gson = new Gson();
        String json = gson.toJson(m_fuelInfoCaches);
        WriteJsonToFile(json, m_externalFuelInfoFile);

        json = gson.toJson(m_fuelDistanceCache);
        WriteJsonToFile(json, m_externalFuelDistanceInfoFile);
    }

    private void WriteJsonToFile(String json, String fileName) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(fileName);
            writer.write(json);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }finally {
            if(writer != null){
                try {
                    writer.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public void LoadFromFile() {
        LoadFuelInfoFromFile();
        LoadFuelDistanceInfoFromFile();
    }

    private void LoadFuelInfoFromFile() {
        try {
            FileReader fileReader = new FileReader(m_externalFuelInfoFile);
            Gson gson = new Gson();
            m_fuelInfoCaches = gson.fromJson(fileReader, new TypeToken<ArrayList<FuelInfoCache>>(){}.getType());
            fileReader.close();
        } catch (IOException e) {
        }
    }

    private void LoadFuelDistanceInfoFromFile() {
        try {
            FileReader fileReader = new FileReader(m_externalFuelDistanceInfoFile);
            Gson gson = new Gson();
            m_fuelDistanceCache = gson.fromJson(fileReader, new TypeToken<ArrayList<FuelDistanceInfoCache>>(){}.getType());
            fileReader.close();
        } catch (IOException e) {
        }
    }

    private void EnsureCacheExistence() {
        if (m_fuelInfoCaches == null){
            m_fuelInfoCaches = new ArrayList<FuelInfoCache>();
        }
        if(m_fuelDistanceCache == null){
            m_fuelDistanceCache = new ArrayList<FuelDistanceInfoCache>();
        }
    }
}
