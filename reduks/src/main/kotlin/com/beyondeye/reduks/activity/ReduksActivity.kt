package com.beyondeye.reduks.activity

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.Reduks
import com.beyondeye.reduks.Store
import com.beyondeye.reduks.StoreSubscriber

interface ReduksActivity<S> {
    var reduks: Reduks<S>
    /**
     * return the initial state of the activity
     */
    fun activityStartState(): S

    /**
     * return the initial action to dispatch to the RxStore in onCreate
     */
    fun activityStartAction(): Any

    /**
     * return the activity state reducer
     */
    fun getActivityStateReducer(): Reducer<S>

    /**
     * return the activity main store subscriber
     */
    fun getActivityStoreSubscriber(): (Store<S>) -> StoreSubscriber<S>

}