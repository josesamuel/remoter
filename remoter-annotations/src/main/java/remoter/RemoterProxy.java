package remoter;

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

}
