package com.beyondeye.reduks.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.*
import com.beyondeye.reduks.rx.RxStore
import com.beyondeye.reduks.rx.RxStoreSubscriber
import com.beyondeye.reduksAndroid.activity.ActionRestoreState
import rx.subscriptions.CompositeSubscription

/**
 * An activity base class for avoiding writing boilerplate code for handling RxJava subscriptions and handling save and restoring reduks state
 * on onSaveInstanceState/onRestoreInstanceState activity life-cycle events
 * automatically handle save and restore of store state on activity recreation using a special custom action [ActionRestoreState]
 * Created by daely on 6/13/2016.
 */
abstract class RxReduksActivity<S>: AppCompatActivity() {
    lateinit var reduks: Reduks<S>
    lateinit var allActivitySubscriptions: CompositeSubscription //all rx subscriptions
    var isGetActivityRxStoreSubscriberCalled =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allActivitySubscriptions= CompositeSubscription()

        reduks=initReduks(RxStore.Creator<S>(allActivitySubscriptions))

        if(!isGetActivityRxStoreSubscriberCalled) throw IllegalArgumentException("It seems that you have overridden getActivityStoreSubscriber(): you should override getActivityRxStoreSubscriber() instead!! ")
    }

    /**
     * function that create the reduks module that should control this activity
     * If your activity also inherit from SingleModuleReduksActivity, then you can simply
     * define this function as
     * override fun initReduks(storeCreator:StoreCreator<S>) = initReduksSingleModule(storeCreator)
     */
    abstract fun initReduks(storeCreator:StoreCreator<S>): Reduks<S>

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        allActivitySubscriptions.unsubscribe()
        super.onDestroy()
    }
    fun getActivityStoreSubscriber(store: Store<S>): StoreSubscriber<S> {
        isGetActivityRxStoreSubscriberCalled =true
        return getActivityRxStoreSubscriber(store)
    }
    /**
     * return the activity main store subscriber
     */
    abstract  fun getActivityRxStoreSubscriber(store: Store<S>): RxStoreSubscriber<S>

    override fun onSaveInstanceState(outState: Bundle?) {
        ActionRestoreState.saveReduksState(reduks,outState)
        super.onSaveInstanceState(outState)
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        ActionRestoreState.restoreReduksState(reduks,savedInstanceState)
    }

}