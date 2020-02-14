package util.remoter.service;

import android.os.Parcel;
import android.os.Parcelable;

public class SimpleParcelable implements Parcelable {

    public static final Creator<SimpleParcelable> CREATOR = new Creator<SimpleParcelable>() {
        @Override
        public SimpleParcelable createFromParcel(Parcel in) {
            return new SimpleParcelable(in);
        }

        @Override
        public SimpleParcelable[] newArray(int size) {
            return new SimpleParcelable[size];
        }
    };

    private int intValue;
    private String stringValue;

    public SimpleParcelable(){
    }

    public SimpleParcelable(String stringValue, int intValue) {
        this.stringValue = stringValue;
        this.intValue = intValue;
    }

    protected SimpleParcelable(Parcel in) {
        readFromParcel(in);
    }

    public int getIntValue() {
        return intValue;
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(intValue);
        parcel.writeString(stringValue);
    }

    public void readFromParcel(Parcel parcel){
        intValue = parcel.readInt();
        stringValue = parcel.readString();
    }


    @Override
    public String toString() {
        return "SimpleParcelable " + intValue +" " + stringValue;
    }
}
