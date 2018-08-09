package com.beyondeye.reduks

import java.lang.ref.WeakReference

/**
 * class that hold a weak reference to a dispatcher function,
 * it is useful in android when we want to allow to dispatch actions
 * from objects that do not have direct access to the parent activity,
 * while avoiding potential memory leaks, and also encapsulating the
 * data type of a dispatcher function
 */
class DispatcherFn(fn_:(action:Any)->Any) {
    val fn=WeakReference(fn_)
    /**
     * just an alias for [invoke] so that we can use [DispatcherFn] as it was
     * the source [Reduks] object
     */
    fun dispatch(action:Any):Any?=invoke(action)
    operator fun  invoke(action:Any):Any? {
        return fn.get()?.run {invoke(action) }
    }
}