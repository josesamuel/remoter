package util.remoter.wrapper;

import remoter.annotations.Remoter;
import util.remoter.service.IBaseA;
import util.remoter.service.IBaseB;

/**
 * Example of a marker remoter interface that specifies other interfaces that should generate remoter proxy stub
 * <p>
 * In this case no proxy/stub gets generate for BaseA, but it gets generated for IBaseA and IBaseB
 */
@Remoter(classesToWrap = {IBaseA.class, IBaseB.class})
interface BaseA {
}
