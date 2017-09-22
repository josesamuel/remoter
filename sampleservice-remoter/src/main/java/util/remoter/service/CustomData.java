package util.remoter.service;

import android.os.Parcelable;

import org.parceler.Parcel;
import org.parceler.Parcels;

import java.util.List;

/**
 * For testing @Parcel (parceler)
 */
@Parcel
public class CustomData {

    CustomData2 customData2;
    CustomData2[] customData2Array;
    List<CustomData2> customData2List;
    int intData;
    TestEnum enumData;

    public CustomData2 getCustomData2() {
        return customData2;
    }

    public CustomData2[] getCustomData2Array() {
        return customData2Array;
    }

    public int getIntData() {
        return intData;
    }

    public TestEnum getEnumData() {
        return enumData;
    }


    public void setCustomData2(CustomData2 customData2) {
        this.customData2 = customData2;
    }

    public void setCustomData2Array(CustomData2[] customData2Array) {
        this.customData2Array = customData2Array;
    }

    public void setCustomData2List(List<CustomData2> customData2List) {
        this.customData2List = customData2List;
    }

    public void setIntData(int intData) {
        this.intData = intData;
    }

    public void setEnumData(TestEnum enumData) {
        this.enumData = enumData;
    }
}
