package util.remoter.service;

import remoter.annotations.Remoter;

@Remoter
public interface ISampleServiceListener {
    void onEcho(String echo);
}
