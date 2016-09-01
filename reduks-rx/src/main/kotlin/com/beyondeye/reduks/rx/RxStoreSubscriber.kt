package com.beyondeye.reduks.rx

import android.util.Log
import com.beyondeye.reduks.StoreSubscriber



/**
 * The only advantage of using RxStoreSubscriber instead of using directlt rx.Subscriber is encapsulation and that
 *  we avoid the need to override all onNext,onCompleted,onError methods
 * we just need to override [RxStoreSubscriber.onStateChange] that is binded to onNext
 */
abstract class RxStoreSubscriber<S>(val store:RxStore<S>): rx.Subscriber<S>(), StoreSubscriber<S> {
    /**
     * RxJava onNext is called whenever a new state update is posted in [RxStore.stateChanges], so we pass it
     * on to onStateChange callback
     */
    override fun onNext(s: S) {
        onStateChange()
    }
    override fun onCompleted() {
        //do nothing
    }
    override fun onError(e: Throwable?) {
        Log.e("RxStoreSubscriber","error on previous action dispatch detected: "+e?.message)
    }
}