package util.remoter.service;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by jmails on 8/20/17.
 */

public class FooParcelable implements Parcelable {

    public static final Creator<FooParcelable> CREATOR = new Creator<FooParcelable>() {
        @Override
        public FooParcelable createFromParcel(Parcel in) {
            return new FooParcelable(in);
        }

        @Override
        public FooParcelable[] newArray(int size) {
            return new FooParcelable[size];
        }
    };

    private int intValue;
    private String stringValue;

    public FooParcelable(){
    }

    public FooParcelable(String stringValue, int intValue) {
        this.stringValue = stringValue;
        this.intValue = intValue;
    }

    protected FooParcelable(Parcel in) {
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


}
