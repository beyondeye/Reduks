package com.beyondeye.reduks;

/**
 * keep this interface in java to leverage kotlin SAM conversion (lambda automatical converted to implementation of java interface)
 */
public interface Middleware<S> {
    Object dispatch(Store<S> store, NextDispatcher next, Object action);
}
