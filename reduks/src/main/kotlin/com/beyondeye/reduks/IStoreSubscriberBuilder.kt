package com.beyondeye.reduks

/**
 * single method interface, mainly used because kotlin does not support yet type alias for function types
 */
interface IStoreSubscriberBuilder<S> {
    fun build(store: Store<S>): IStoreSubscriber<S>
}
