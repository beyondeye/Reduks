package com.beyondeye.reduks;

/**
 *  keep this interface in java to leverage kotlin SAM conversion (lambda automatical converted to implementation of java interface)
 *
 * Created by daely on 5/24/2016.
 */
public interface Thunk<S> extends Action {
    Object execute(NextDispatcher dispatcher, S state);
}
