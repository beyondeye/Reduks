package com.beyondeye.reduks

object ReduksInternalLogUtils {
    fun reportErrorInReducer(s:Store<*>, e:Throwable) {
        s.errorLogFn?.invoke("REDUKS: exception while running reducer: ${e}")
    }

    fun reportErrorInSubscriber(s:Store<*>, e:Throwable) {
        s.errorLogFn?.invoke("REDUKS: exception while notifying subscriber: ${e}")
    }
}