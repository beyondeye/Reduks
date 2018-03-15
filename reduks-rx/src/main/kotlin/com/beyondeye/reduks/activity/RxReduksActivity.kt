package com.beyondeye.reduks.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.*
import com.beyondeye.reduks.rx.RxStore
import com.beyondeye.reduks.rx.RxStoreSubscriber
import com.beyondeye.reduksAndroid.activity.ActionRestoreState
import com.beyondeye.reduksAndroid.activity.ReduksActivity
import rx.subscriptions.CompositeSubscription

/**
 * An activity base class for avoiding writing boilerplate code for handling RxJava subscriptions and handling save and restoring reduks state
 * on onSaveInstanceState/onRestoreInstanceState activity life-cycle events
 * automatically handle save and restore of store state on activity recreation using a special custom action [ActionRestoreState]
 * Created by daely on 6/13/2016.
 */
abstract class RxReduksActivity<S>:  ReduksActivity<S>, AppCompatActivity() {
    lateinit override var reduks: Reduks<S>
    lateinit var allActivitySubscriptions: CompositeSubscription //all rx subscriptions
    var isGetActivityRxStoreSubscriberCalled =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allActivitySubscriptions= CompositeSubscription()

        reduks=initReduks()

        if(!isGetActivityRxStoreSubscriberCalled) throw IllegalArgumentException("It seems that you have overridden getActivityStoreSubscriber(): you should override getActivityRxStoreSubscriber() instead!! ")
    }

    override fun <T> storeCreator(): StoreCreator<T> = RxStore.Creator<T>(allActivitySubscriptions)

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
        ActionRestoreState.saveReduksState(this,reduks,outState)
        super.onSaveInstanceState(outState)
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        ActionRestoreState.restoreReduksState(this,reduks,savedInstanceState)
    }

}