package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.UnwrapActionMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware

/**
 * all data needed for creating a ReduksModule
 * Created by daely on 7/30/2016.
 */
abstract class ReduksModuleDef<S> {
    /**
     * factory method for store
     */
    abstract val  storeFactory:StoreFactory<S>

    /**
     * return the initial state
     */
    abstract val initialState: S

    /**
     * return the initial action to dispatch to the Store
     */
    abstract val startAction: Any

    /**
     * return the state reducer
     */
    abstract val stateReducer: Reducer<S>

    /**
     * return the main store subscriber
     */
    abstract fun getStoreSubscriber(): (Store<S>) -> StoreSubscriber<S>
}