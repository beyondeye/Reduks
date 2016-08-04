package com.beyondeye.reduks.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.Reduks
import com.beyondeye.reduks.Store
import com.beyondeye.reduks.StoreSubscriber
import com.beyondeye.reduks.StoreSubscriberBuilder
import com.beyondeye.reduks.modules.ReduksModule
import com.beyondeye.reduks.rx.RxStore
import com.beyondeye.reduks.rx.RxStoreSubscriber
import rx.subscriptions.CompositeSubscription

/**
 * Created by daely on 6/13/2016.
 */
abstract class RxReduksActivity<S>: AppCompatActivity(), ReduksActivity<S> {
    lateinit override var reduks: Reduks<S>
    lateinit var allActivitySubscriptions: CompositeSubscription //all rx subscriptions
    var isGetActivityRxStoreSubscriberCalled =false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        allActivitySubscriptions= CompositeSubscription()
        //       startKovenant()
        reduks = ReduksModule<S>(
                ReduksModule.Def<S>(
                        activityReduksContext(),
                        RxStore.Factory<S>(allActivitySubscriptions),
                        activityStartState(),
                        activityStartAction(),
                        getActivityStateReducer(),
                        StoreSubscriberBuilder<S> {getActivityStoreSubscriber(it)})
        )
        if(!isGetActivityRxStoreSubscriberCalled) throw IllegalArgumentException("It seems that you have overridden getActivityStoreSubscriber(): you should override getActivityRxStoreSubscriber() instead!! ")
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
        allActivitySubscriptions.unsubscribe()
        super.onDestroy()
    }
    override fun getActivityStoreSubscriber(store: Store<S>): StoreSubscriber<S> {
        isGetActivityRxStoreSubscriberCalled =true
        return getActivityRxStoreSubscriber(store)
    }
    /**
     * return the activity main store subscriber
     */
    abstract  fun getActivityRxStoreSubscriber(store: Store<S>): RxStoreSubscriber<S>

}