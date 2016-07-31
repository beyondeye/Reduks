package com.beyondeye.reduks

/**
 * Factory for some specific Store type
 * Created by daely on 7/31/2016.
 */
interface StoreFactory<S> {
    fun newStore(initialState:S, reducer: Reducer<S>):Store<S>
}