package com.beyondeye.reduks.modules

import com.beyondeye.reduks.IStoreSubscription

class MultiStoreSubscription(vararg subscriptions_: IStoreSubscription) : IStoreSubscription {
    val subscriptions=subscriptions_
    override fun unsubscribe() {
        subscriptions.forEach { it.unsubscribe() }
    }
}