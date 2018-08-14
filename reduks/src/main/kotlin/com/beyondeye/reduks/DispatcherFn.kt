package com.beyondeye.reduks

import java.lang.ref.WeakReference

interface DispatcherFn {
    val fn: ((action: Any) -> Any)?
    /**
     * just an alias for [invoke] so that we can use [DispatcherFn] as it was
     * the source [Reduks] object
     */
    fun dispatch(action: Any): Any?

    operator fun invoke(action: Any): Any?

    companion object {
        fun instance(fn: (action: Any) -> Any, isWeakRef: Boolean) = if (isWeakRef) DispatcherFn_weakref(fn) else DispatcherFn_ref(fn)
    }
}

/**
 * class that hold a weak reference to a dispatcher function,
 * it is useful in android when we want to allow to dispatch actions
 * from objects that do not have direct access to the parent activity,
 * while avoiding potential memory leaks, and also encapsulating the
 * data type of a dispatcher function.
 */
class DispatcherFn_weakref(fn_:(action:Any)->Any) : DispatcherFn {
    val fnw=WeakReference(fn_)
    override val fn: ((action: Any) -> Any)?
        get() = fnw.get()

    override fun dispatch(action:Any):Any?=invoke(action)
    override operator fun  invoke(action:Any):Any? {
        return fnw.get()?.run {invoke(action) }
    }
}


/**
 * another implementation of [DispatcherFn] that keep a regular reference (not weak reference) to
 * the dispatcher
 */
class DispatcherFn_ref(fn_:(action:Any)->Any) : DispatcherFn {
    override val fn: ((action: Any) -> Any)?=fn_
    /**
     * just an alias for [invoke] so that we can use [DispatcherFn] as it was
     * the source [Reduks] object
     */
    override fun dispatch(action:Any):Any?=invoke(action)
    override operator fun  invoke(action:Any):Any? {
        return fn.invoke(action)
    }
}