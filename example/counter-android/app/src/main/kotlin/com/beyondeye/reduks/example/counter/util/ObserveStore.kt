package com.beyondeye.reduks.example.counter.util

import com.beyondeye.reduks.Store
import com.beyondeye.reduks.StoreSubscriber
import com.beyondeye.reduks.StoreSubscription
import com.beyondeye.reduks.subscribe

/**
 * Created by kittinunf on 9/2/16.
 */

fun <S> observeStore(store: Store<S>, onChanged: (S) -> Unit): StoreSubscription {
    var currentState: S? = null

    fun handle() {
        val newState = store.state
        if (newState != currentState) {
            currentState = newState
            onChanged(currentState!!)
        }
    }

    val unsubscribe = store.subscribe(StoreSubscriber { handle() })
    handle()
    return unsubscribe
}
