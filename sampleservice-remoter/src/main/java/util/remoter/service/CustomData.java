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


    void test(){
        Parcelable p = Parcels.wrap(this);
        String c = Parcels.unwrap(p);

    }

}
