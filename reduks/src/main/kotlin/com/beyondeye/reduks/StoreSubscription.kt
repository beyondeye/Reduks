package com.beyondeye.reduks

/**
 * single method interface, mainly used because kotlin does not support yet type alias for function types
 */
interface StoreSubscription {
    fun unsubscribe()
}

fun StoreSubscription.addToList(subscriptions:MutableList<StoreSubscription>) {
    subscriptions.add(this)
}

fun MutableList<StoreSubscription>.unsubscribe() {
    this.forEach { it.unsubscribe() }
    this.clear()
}