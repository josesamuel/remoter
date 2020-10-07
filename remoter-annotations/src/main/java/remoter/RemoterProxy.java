package remoter;

import java.util.Map;

/**
 * Represents a remote proxy. This will be implemented by the Remoter generated Proxy classes.
 */
public interface RemoterProxy {

    /**
     * Register a {@link RemoterProxyListener}
     */
    void registerProxyListener(RemoterProxyListener listener);


    /**
     * Un register a {@link RemoterProxyListener}
     */
    void unRegisterProxyListener(RemoterProxyListener listener);

    /**
     * Checks whether the remote side is still alive
     *
     * @see #registerProxyListener(RemoterProxyListener)
     */
    boolean isRemoteAlive();


    /**
     * Destroys any stub created while sending the given object through this proxy.
     *
     * @see #destroyProxy()
     */
     void destroyStub(Object object);

    /**
     * Call to destroy the proxy. Proxy should not be used after this. This also clears any stubs
     * that are send using this proxy
     */
    void destroyProxy();

    /**
     * Set any global properties to be send with all remote calls.
     * At the service side this can be obtained using {@link RemoterGlobalProperties#get(String)}
     */
    void setRemoterGlobalProperties(Map<String, Object> properties);
}
