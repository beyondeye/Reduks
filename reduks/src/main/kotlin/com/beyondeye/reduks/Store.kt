package com.beyondeye.reduks

interface Store<S> {
    val state: S
    /**
     * dispatch the action to the store and return it
     * An action can be of Any type
     */
    var dispatch: (action: Any) -> Any
}
