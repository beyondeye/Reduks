package com.brianegan.bansa;

/**
 * keep this interface in java to leverage kotlin SAM conversion (lambda automatical converted to implementation of java interface)
 */
public interface Reducer<S> {
    S reduce(S state, Object action);
}
