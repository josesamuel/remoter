package util.remoter.service;

import remoter.annotations.Remoter;

/**
 * To test interface extensions
 */
@Remoter
public interface IExtD extends IBaseA, IBaseB {
    float echoFloat(float s);
    float echoFloat(float s, float s2);
}
