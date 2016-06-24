package com.beyondeye.reduks

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