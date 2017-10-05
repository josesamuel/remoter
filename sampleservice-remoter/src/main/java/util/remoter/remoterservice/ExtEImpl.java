package util.remoter.remoterservice;

import util.remoter.service.IExtE;

/**
 * For testing @Remote that extends other interfaces
 */
public class ExtEImpl implements IExtE {
    @Override
    public int echoInt(int s) {
        return s;
    }

    @Override
    public int echoInt(int s, int s2) {
        return s + s2;
    }

    @Override
    public String echoString(String s) {
        return s;
    }

    @Override
    public String echoString(String s, String s2) {
        return s + s2;
    }

    @Override
    public float echoFloat(float s) {
        return s;
    }

    @Override
    public float echoFloat(float s, float s2) {
        return s + s2;
    }

    @Override
    public long echoLong(long s) {
        return s;
    }

    @Override
    public long echoLong(long s, long s2) {
        return s + s2;
    }
}
