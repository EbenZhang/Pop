package com.ezhang.pop.model;


import android.os.Parcel;
import android.os.Parcelable;

public class FuelInfo implements Parcelable {
    public float price;
    public String brand;
    private String m_address;
    public String latitude;
    public String longitude;
    public String tradingName;
    private String m_suburb;

    private FuelInfo(Parcel in) {
        price = in.readFloat();
        brand = in.readString();
        m_address = in.readString();
        latitude = in.readString();
        longitude = in.readString();
        tradingName = in.readString();
        m_suburb = in.readString();
    }

    public FuelInfo() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public int describeContents() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeFloat(price);
        dest.writeString(brand);
        dest.writeString(m_address);
        dest.writeString(latitude);
        dest.writeString(longitude);
        dest.writeString(tradingName);
        dest.writeString(m_suburb);
    }

    public static final Parcelable.Creator<FuelInfo> CREATOR = new Parcelable.Creator<FuelInfo>() {
        public FuelInfo createFromParcel(Parcel in) {
            return new FuelInfo(in);
        }

        public FuelInfo[] newArray(int size) {
            return new FuelInfo[size];
        }
    };

    public void SetSuburb(String suburb) {
        m_suburb = suburb;
    }
    public String GetAddress(){
        return m_address + ", " + m_suburb;
    }

    public void SetAddressWithoutSuburb(String address){
        m_address = address;
    }
}
