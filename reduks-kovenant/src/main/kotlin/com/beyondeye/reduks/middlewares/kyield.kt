package com.beyondeye.reduks.middlewares

import java.util.*
import java.util.concurrent.SynchronousQueue

/**
 * Created by daely on 10/23/2016.
 */
/**
 * Creates a yield function or method
 * To yield a value use "ret yield myObj" or "yield(myObj)" expressions
 * To return from method just do return
 * WARNING: do not throw InterruptedException in method body
 * and do not wrap yield expression with try-catch muting InterruptedException
 * ORIGINAL code is from https://bitbucket.org/Nerzhul500/kyield/src
 * see discussion of implementation here developers-club.com/posts/168571/
 **/
fun <T: Any>yieldfun(body: YieldContext<T>.() -> Unit): Iterable<T> = YieldIterable<T>(body)

interface YieldContext<T> {
    infix fun kyield(value: T): Unit //kyield, because of name clas
    val ret: YieldContext<T>
}

private class YieldIterable<T:Any>(val body: YieldContext<T>.() -> Unit): Iterable<T>{
    override fun iterator(): Iterator<T> {
        return YieldIterator<T>(body)
    }
}

private class YieldFinalizedException: Exception() {
}

private open class Message {
}

private class ValueMessage( val value: Any): Message() {
}

private class CompletedMessage: Message() {
}

private class ExceptionThrownMessage( val exception: Throwable): Message() {
}

private class YieldIterator<T:Any> (val body: YieldContext<T>.() -> Unit): Iterator<T>, YieldContext<T> {
    private var thread: Thread? = null
    private val resultQueue = SynchronousQueue<Message>()
    private val continuationSync = SynchronousQueue<Any>()
    private var currentMessage: Message? = null

    init {
        val r = Runnable {
                try {
                    continuationSync.take()
                    body()
                    resultQueue.put(CompletedMessage())
                }
                catch (e: InterruptedException) {
                    // if not all items were iterated so yield will wait for signal and finalizer should
                    // interrupt the thread to exit
                }
                catch (e: Throwable) {
                    resultQueue.put(ExceptionThrownMessage(e))
                }
        }
        thread = Thread(r)
        thread!!.start()
    }

    override fun kyield(value: T) {
        resultQueue.put(ValueMessage(value))
        continuationSync.take()
    }

    override val ret: YieldContext<T> = this

     override fun next(): T {
        evaluateNext()
        if (currentMessage is ExceptionThrownMessage)
            throw (currentMessage as ExceptionThrownMessage).exception
        if (currentMessage !is ValueMessage)
            throw NoSuchElementException()
        val value = (currentMessage as ValueMessage).value as T
        currentMessage = null
        return value
    }
     override fun hasNext(): Boolean {
        evaluateNext()
        if (currentMessage is ExceptionThrownMessage)
            throw (currentMessage as ExceptionThrownMessage).exception
        return currentMessage is ValueMessage
    }

    private val dummy = Any()

    private inline fun evaluateNext() {
        if (currentMessage == null) {
            continuationSync.put(dummy)
            currentMessage = resultQueue.take()
        }
    }

    protected fun finalize() {
        thread?.interrupt()
    }
}