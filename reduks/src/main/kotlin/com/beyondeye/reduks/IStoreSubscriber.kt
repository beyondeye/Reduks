package com.beyondeye.reduks

/**
 * single method interface, mainly used because kotlin does not support yet type alias for function types
 */
interface IStoreSubscriber<S> {
    fun onStateChange()
}
