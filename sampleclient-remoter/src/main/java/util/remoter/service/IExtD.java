package util.remoter.service;

/**
 * To test interface extensions
 */
public interface IExtD extends IBaseA, IBaseB {
    float echoFloat(float s);
    float echoFloat(float s, float s2);
}
