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
                    StoreSubscriberBuilderFn<S> {store -> getActivityStoreSubscriber(store)})
    )
    /**
     * module id used for reduks
     * by default defined by the activity class name
     */
    fun activityReduksContext(): ReduksContext=ReduksContext(this.javaClass.simpleName)
    /**
     * return the initial state
     */
    fun activityStartState(): S

    /**
     * return the initial action to dispatch to the Store in onCreate
     * or null, if no initial action need to be dispatched
     */
    fun activityStartAction(): Any?=INIT()

    /**
     * return the activity state reducer
     */
    fun getActivityStateReducer(): Reducer<S>

    /**
     * return the activity main store subscriber
     */
    fun getActivityStoreSubscriber(store: Store<S>): StoreSubscriber<S>

}