package com.beyondeye.reduks

import com.beyondeye.reduks.modules.ReduksContext

/**
 * Redux module containing all Reduks components: store and its (main) subscriber
 * Created by daely on 6/8/2016.
 */
interface Reduks<State> {
    val ctx: ReduksContext
    val store: Store<State>
    val storeSubscriber: StoreSubscriber<State>
    val storeSubscription: StoreSubscription
    /**
     * call dispatch on the store object
     */
    fun dispatch(action:Any) = store.dispatch(action)
}
