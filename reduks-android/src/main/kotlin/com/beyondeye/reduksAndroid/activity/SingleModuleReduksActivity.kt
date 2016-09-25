package com.beyondeye.reduksAndroid.activity

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ReduksContext
import com.beyondeye.reduks.modules.ReduksModule

/**
 * an interface used for making it more convenient to define all components of a single module reduks android activity
 * just make your Activity inherit from this interface and implement its methods
 * Also for saving up some more boilerplate code, you can make your activity inherit from KovenantReduksActivity (Kovenant based store)
 * or RxReduksActivity (RxJava based story)
 */
interface SingleModuleReduksActivity<S> {
    fun initReduksSingleModule(storeCreator:StoreCreator<S>): Reduks<S> = ReduksModule<S>(
            ReduksModule.Def<S>(
                    activityReduksContext(),
                    storeCreator,
                    activityStartState(),
                    activityStartAction(),
                    combineReducers(getActivityStateReducer(), ActionRestoreState.getRestoreStateReducer()),
                    StoreSubscriberBuilder<S> {getActivityStoreSubscriber(it)})
    )
    /**
     * module id used for reduks
     */
    fun activityReduksContext(): ReduksContext
    /**
     * return the initial state
     */
    fun activityStartState(): S

    /**
     * return the initial action to dispatch to the Store in onCreate
     */
    fun activityStartAction(): Any

    /**
     * return the activity state reducer
     */
    fun getActivityStateReducer(): IReducer<S>

    /**
     * return the activity main store subscriber
     */
    fun getActivityStoreSubscriber(store: Store<S>): IStoreSubscriber<S>

}