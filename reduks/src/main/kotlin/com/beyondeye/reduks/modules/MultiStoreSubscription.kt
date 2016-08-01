package com.beyondeye.reduks.modules

import com.beyondeye.reduks.StoreSubscription

class MultiStoreSubscription(vararg subscriptions_: StoreSubscription) : StoreSubscription {
    val subscriptions=subscriptions_
    override fun unsubscribe() {
        subscriptions.forEach { it.unsubscribe() }
    }
}