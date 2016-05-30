package com.brianegan.bansa.rx
import com.brianegan.bansa.StoreSubscription
import rx.android.schedulers.AndroidSchedulers
/**
 * Created by daely on 5/23/2016.
 * note: the constructor take as input an [rx.Subscriber] which is a base class for [RxStoreSubscriber]
 */

class RxStoreSubscription<S>(val rxStore:RxStore<S>, val subscriber:rx.Subscriber<S>, subscribeOnAndroidMainThread:Boolean) : StoreSubscription {
    var storeSubscription = rx.subscriptions.Subscriptions.unsubscribed()
    fun storeChangesSubscribed() =!storeSubscription.isUnsubscribed()
    init {
        if(subscribeOnAndroidMainThread)
          storeSubscription =rxStore.stateChanges.subscribeOn(AndroidSchedulers.mainThread()).subscribe(subscriber)
        else
          storeSubscription =rxStore.stateChanges.subscribe(subscriber)
        rxStore.allRxSubscriptions?.add(subscriber)
    }
    override fun unsubscribe() {
        if(storeSubscription.isUnsubscribed) return;
        storeSubscription.unsubscribe()
        rxStore.allRxSubscriptions?.remove(subscriber)
    }
}