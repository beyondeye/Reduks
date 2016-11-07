package com.beyondeye.reduks

import com.beyondeye.reduks.modules.ReduksContext

/**
 * Redux module containing all Reduks components: store and its (main) subscriber
 * Created by daely on 6/8/2016.
 */
interface Reduks<State> {
    val ctx: ReduksContext
    val store: Store<State>
    val storeSubscriber: StoreSubscriber<State>
    val storeSubscription: StoreSubscription
}


/**
 * a shortcut for getting the current state from the store object
 */
val <S>Reduks<S>.state:S get()=store.state

/**
 * a shortcur for calling dispatch on the store object
 */
fun <S>Reduks<S>.dispatch(action:Any) = store.dispatch(action)
