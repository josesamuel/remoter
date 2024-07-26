package util.remoter.service;

import java.util.List;

import remoter.annotations.Remoter;

@Remoter
public interface GenericInterface <AA extends List<Integer>> {
    void onNext(AA list);
}
