package remoter;

/**
 * Listener to get notified about changes in a {@link RemoterProxy}
 */
public interface RemoterProxyListener {

    /**
     * Called when the remote proxy connection is lost
     */
    void onProxyDead();
}
