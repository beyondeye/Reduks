package com.beyondeye.reduks;

/**
 * keep this interface in java to leverage kotlin SAM conversion (lambda automatical converted to implementation of java interface)
 */
public interface StoreSubscriberBuilder<S> {
    StoreSubscriber<S> build(Store<S> store);
}
