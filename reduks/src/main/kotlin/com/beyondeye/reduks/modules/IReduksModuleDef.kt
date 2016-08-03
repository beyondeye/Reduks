package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.StoreFactory
import com.beyondeye.reduks.StoreSubscriberBuilder

/**
 * Created by daely on 8/3/2016.
 */
interface IReduksModuleDef<State>
{
    /**
     * factory method for store
     */
    val  storeFactory: StoreFactory<State>
    /**
     * return the initial state
     */
    val initialState: State
    /**
     * return the initial action to dispatch to the Store
     */
    val startAction: Any
    /**
     * return the state reducer
     */
    val stateReducer: Reducer<State>
    /**
     * return the main store subscriber
     * we pass as argument the store itself, so that we can create an object that implement the
     * StoreSubscriber interface that keep a reference to the store itself, in case the we need call dispatch
     * in the subscriber
     */
    val subscriberBuilder: StoreSubscriberBuilder<State>
}
