package com.beyondeye.reduks

/**
 * Redux based on [SimpleStore]
 * Created by daely on 6/8/2016.
 */
interface Reduks<State> {
    val store: Store<State>
    val storeSubscriber: StoreSubscriber<State>
    val storeSubscription: StoreSubscription
    /**
     * call dispatch on the store object
     */
    fun dispatch(action:Any) = store.dispatch(action)
}
