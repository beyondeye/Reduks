package com.beyondeye.reduks.rx

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.Reduks
import com.beyondeye.reduks.ReduksActivity
import com.beyondeye.reduks.Store
import com.beyondeye.reduks.StoreSubscriber
import rx.subscriptions.CompositeSubscription

/**
 * Created by daely on 6/13/2016.
 */
abstract class RxReduksActivity<S>: AppCompatActivity(), ReduksActivity<S> {
    lateinit override var reduks: Reduks<S>
    lateinit var allActivitySubscriptions: CompositeSubscription //all rx subscriptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allActivitySubscriptions= CompositeSubscription()
 //       startKovenant()
        reduks = RxReduks<S>(activityStartState(), activityStartAction(), getActivityStateReducer(), getActivityRxStoreSubscriber(), allActivitySubscriptions)
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
//        stopKovenant()
        allActivitySubscriptions.unsubscribe()
        super.onDestroy()
    }
    override fun getActivityStoreSubscriber(): (Store<S>) -> StoreSubscriber<S> {
        return getActivityRxStoreSubscriber()
    }
    /**
     * return the activity main store subscriber
     */
    abstract  fun getActivityRxStoreSubscriber(): (Store<S>) -> RxStoreSubscriber<S>

}