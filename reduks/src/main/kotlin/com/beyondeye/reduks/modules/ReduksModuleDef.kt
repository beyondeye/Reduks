package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * all data needed for creating a ReduksModule
 * Created by daely on 7/30/2016.
 */
data class ReduksModuleDef<S>(
    /**
     * factory method for store
     */
    val  storeFactory:StoreFactory<S>,
    /**
     * return the initial state
     */
    val initialState: S,
    /**
     * return the initial action to dispatch to the Store
     */
     val startAction: Any,
    /**
     * return the state reducer
     */
    val stateReducer: Reducer<S>,
    /**
     * return the main store subscriber
     * we pass as argument the store itself, so that we can create an object that implement the
     * [StoreSubscriber] interface that keep a reference to the store itself, in case the we need call dispatch
     * in the subscriber
     */
    val subscriberBuilder: StoreSubscriberBuilder<S>)
