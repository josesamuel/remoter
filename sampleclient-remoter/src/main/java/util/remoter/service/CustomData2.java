package util.remoter.service;


import org.parceler.Parcel;

@Parcel
public class CustomData2 {
    int intData;
    TestEnum enumData;


    public int getIntData() {
        return intData;
    }

    public void setIntData(int intData) {
        this.intData = intData;
    }

    public TestEnum getEnumData() {
        return enumData;
    }

    public void setEnumData(TestEnum enumData) {
        this.enumData = enumData;
    }
}
