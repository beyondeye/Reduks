package com.beyondeye.reduks.experimental.utils

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.CoroutineContext
import kotlin.coroutines.experimental.suspendCoroutine

/**
 * allow to define callback wrappers that are protected  from accidental multiple calls to resume/resumeWithException
 * Created by daely on 3/30/2017.
 */
class WrappedContinuation<T>(val c: Continuation<T>) : Continuation<T> {
    var isResolved = false
    override val context: CoroutineContext
        get() = c.context

    override fun resume(value: T) {
        if (!isResolved) {
            isResolved = true
            c.resume(value)
        }
    }

    override fun resumeWithException(exception: Throwable) {
        if (!isResolved) {
            isResolved = true
            c.resumeWithException(exception)
        }
    }

}

 inline suspend fun <T> suspendCoroutineW(crossinline block: (WrappedContinuation<T>) -> Unit): T =
        suspendCoroutine { c ->
            val wd = WrappedContinuation(c)
            block(wd)
        }

