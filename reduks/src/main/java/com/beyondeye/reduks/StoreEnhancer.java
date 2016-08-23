package com.beyondeye.reduks;

/**
 * get a store creator and return a new enhanced one
 * see https://github.com/reactjs/redux/blob/master/docs/Glossary.md#store-enhancer
 *
 * keep this interface in java to leverage kotlin SAM conversion (lambda automatical converted to implementation of java interface)
 * Created by daely on 8/23/2016.
 */
public interface StoreEnhancer<S> {
    StoreCreator<S> enhance(StoreCreator<S> next);
}
