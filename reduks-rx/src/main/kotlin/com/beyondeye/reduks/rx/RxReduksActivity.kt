package com.beyondeye.reduks.rx

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.Reducer
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant
import rx.subscriptions.CompositeSubscription

/**
 * Created by daely on 6/13/2016.
 */
abstract class RxReduksActivity<S>: AppCompatActivity() {
    lateinit var reduks: RxReduks<S>
    lateinit var allActivitySubscriptions: CompositeSubscription //all rx subscriptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allActivitySubscriptions= CompositeSubscription()
        startKovenant()
        initReduks()
    }
    //override for make this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for make this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }

    override fun onDestroy() {
        stopKovenant()
        allActivitySubscriptions.unsubscribe()
        super.onDestroy()
    }
    fun initReduks() {
        reduks = RxReduks(activityStartState(), activityStartAction(), getActivityStateReducer(), getActivityStoreSubscriber(), allActivitySubscriptions)
    }

    /**
     * return the initial state of the activity
     */
    abstract fun activityStartState():S

    /**
     * return the initial action to dispatch to the [RxStore] in [onCreate]
     */
    abstract fun activityStartAction():Any

    /**
     * return the activity state reducer
     */
    abstract fun getActivityStateReducer(): Reducer<S>


    /**
     * return the activity main store subscriber
     */
    abstract fun getActivityStoreSubscriber():(RxStore<S>) -> RxStoreSubscriber<S>

}