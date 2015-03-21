package com.ezhang.pop.model;

import android.os.Parcel;
import android.os.Parcelable;
import com.ezhang.pop.core.TimeUtil;
import com.ezhang.pop.settings.AppSettings;

import java.util.Date;

public class FuelCacheParam implements Parcelable {
    int m_cachedFuelType = 0;
    @Deprecated
    int m_cachedDay = 0;
    int m_wwsVoucher = 0;
    int m_colesVoucher = 0;
    boolean m_cachedIncludeSurroundings = false;
    String m_cachedSuburb = null;
    String m_cachedDate = null;

    public FuelCacheParam() {
    }

    public FuelCacheParam(Parcel in) {
        m_cachedFuelType = in.readInt();
        m_cachedDay = in.readInt();
        m_wwsVoucher = in.readInt();
        m_colesVoucher = in.readInt();
        m_cachedIncludeSurroundings = in.readByte() != 0;
        m_cachedSuburb = in.readString();
        m_cachedDate = in.readString();
    }

    public static final Parcelable.Creator<FuelCacheParam> CREATOR = new Parcelable.Creator<FuelCacheParam>() {
        public FuelCacheParam createFromParcel(Parcel in) {
            return new FuelCacheParam(in);
        }

        public FuelCacheParam[] newArray(int size) {
            return new FuelCacheParam[size];
        }
    };

    public boolean HitCache(AppSettings settings, String suburb, Date dayOfFuel)
    {
        java.text.SimpleDateFormat df = new java.text.SimpleDateFormat("dd/MM/yyyy");
        boolean hit = m_cachedDate != null && m_cachedDate.equals(df.format(dayOfFuel));
        hit &= m_cachedFuelType == settings.GetFuelType();
        hit &= settings.m_wwsDiscount == m_wwsVoucher;
        hit &= settings.m_colesDiscount == m_colesVoucher;
        hit &= settings.IncludeSurroundings() == m_cachedIncludeSurroundings;
        hit &= m_cachedSuburb != null && suburb.equalsIgnoreCase(m_cachedSuburb);
        return hit;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(m_cachedFuelType);
        parcel.writeInt(m_cachedDay);
        parcel.writeInt(m_wwsVoucher);
        parcel.writeInt(m_colesVoucher);
        parcel.writeByte((byte)(m_cachedIncludeSurroundings?1:0));
        parcel.writeString(m_cachedSuburb);
        parcel.writeString(m_cachedDate);
    }
}