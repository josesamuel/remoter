/**
 * <p>
 * <b>Remoter</b> is an alternative to Android AIDL for Android Remote IPC services using plain java interfaces.
 * </p>
 * <br>
 * <br>
 * Example -
 * <br>
 * <pre><code>
 *
 * //Remoter annotation marks this interface as a remoter service interface
 * {@literal @}Remoter
 *  public interface ISampleService {
 *
 *      //An example where parameters can be in, out or inout
 *      void boolean foo( boolean a,
 *                      {@literal @}ParamIn boolean[] arrayIn,
 *                      {@literal @}ParamOut boolean[] arrayOut,
 *                       boolean[] arrayInOut);
 *
 *      //An example to mark a method as oneway asynchronous method
 *      {@literal @}Oneway
 *      void asynchronusFoo(int x);
 * }
 * </code></pre>
 *
 * <p>
 *     At the <b>client</b> side :
 *     <br>
 *     <br>
 *     Simply wrap the binder that you got from the ServiceConnection with the autogenerated Proxy for your interface
 *<pre><code>
 *     ISampleService sampleService = new ISampleService_Proxy( binder );
 *</code></pre>
 *
 * <br>
 *
 * <p>
 *     At the <b>service</b> side :
 *     <br>
 *     <br>
 *     Wrap the implementation with the autogenerated Stub to covert it as a remote Binder and return that from your service
 *<pre><code>
 *     Binder binder = new ISampleService_Stub( sampleServiceImpl );
 *</code></pre>
 *
 * <br>
 *
 * <p>
 * To add Remoter to your project add these to its gradle <b>dependencies</b>:
 * <br>
 *  <b>api 'com.josesamuel:remoter-annotations:VERSION'</b>
 * <br>
 *  <b>annotationProcessor 'com.josesamuel:remoter:VERSION'</b>
 * </p>
 * <br>
 *
 */
package remoter.annotations;
