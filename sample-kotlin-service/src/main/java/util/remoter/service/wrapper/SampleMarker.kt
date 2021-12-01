package util.remoter.service.wrapper

import remoter.annotations.Remoter
import util.remoter.service.IBaseA
import util.remoter.service.sub.IBaseB


/**
 * Example of a marker remoter interface that specifies other interfaces that should generate remoter proxy stub
 *
 * In this case no proxy/stub gets generate for SampleMarker, but it gets generated for IBaseA and IBaseB
 */
@Remoter(classesToWrap = [IBaseA::class, IBaseB::class])
interface SampleMarker