package com.beyondeye.reduks

import com.beyondeye.reduks.modules.ReduksContext


/**
 * Redux module containing all Reduks components: store and its (main) subscriber
 * Created by daely on 6/8/2016.
 */
interface Reduks<State> {
    val ctx: ReduksContext
    val store: Store<State>
    /**
     * the active subscriptions. for making it easier to unsubscribe by string tag, instead of keeping manually keeping
     * a reference to the StoreSubscription
     */
    val storeSubscriptionsByTag: MutableMap<String, StoreSubscription>
    val busStoreSubscriptionsByTag:MutableMap<String,MutableList<StoreSubscription>>
    companion object {
        val TagMainSubscription="*MAIN*"
    }
}



/**
 * a shortcut for getting the current state from the store object
 */
val <S>Reduks<S>.state:S get()=store.state

/**
 * a shortcurt for calling dispatch on the store object
 */
fun <S>Reduks<S>.dispatch(action:Any) = store.dispatch(action)


fun <S>Reduks<S>.subscribe(subscriptionTag:String, storeSubscriber: StoreSubscriber<S>) {
    unsubscribe(subscriptionTag)
    storeSubscriptionsByTag.put(subscriptionTag, store.subscribe(storeSubscriber))
}
fun <S>Reduks<S>.subscribe(subscriptionTag:String, storeSubscriberBuilder: StoreSubscriberBuilder<S>) {
    unsubscribe(subscriptionTag)
    storeSubscriptionsByTag.put(subscriptionTag, store.subscribe(storeSubscriberBuilder)!!)
}

fun <S>Reduks<S>.unsubscribe(subscriptionTag:String) {
    storeSubscriptionsByTag.remove(subscriptionTag)?.unsubscribe()
}
fun <S>Reduks<S>.unsubscribeAll() {
    storeSubscriptionsByTag.values.forEach { it.unsubscribe() }
    storeSubscriptionsByTag.clear()
}

fun <S>Reduks<S>.getDispatcherFn():DispatcherFn=store.getDispatcherFn()
