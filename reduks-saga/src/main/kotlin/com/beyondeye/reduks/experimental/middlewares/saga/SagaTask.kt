package com.beyondeye.reduks.experimental.middlewares.saga

import kotlinx.coroutines.experimental.Deferred

/**
 * Created by daely on 12/20/2017.
 * The Task interface specifies the result of running a Saga using fork, middleware.run or runSaga.
 */
interface SagaTask<R> {
    val name:String
    /**
     * true if the task hasn't yet returned or thrown an error
     */
    fun isRunning():Boolean

    /**
     * true if the task has been cancelled
     */
    fun isCancelled():Boolean

    /**
     * task return value. null if task is still running
     */
    fun result():R?

    /**
     * task thrown error. null if task is still running
     */
    fun error():Throwable?

    /**
     * a Promise which is either:
    *    resolved with task's return value
    *    rejected with task's thrown error
     */
    fun done():Deferred<R>

    /**
     * Cancels the task (If it is still running)
     */
    fun cancel()

}


class SagaTaskFromDeferred<R>(override val name:String,val d:Deferred<R>): SagaTask<R> {
    override fun isRunning():Boolean = !d.isCompleted

    override fun isCancelled():Boolean =d.isCancelled

    override fun result(): R? {
        return try {
            d.getCompleted()
        } catch (e: Exception) {
            null
        }
    }

    override fun error(): Throwable? {
        return try {
            d.getCompletionExceptionOrNull()
        } catch (e: Exception) {
            null
        }
    }

    override fun done(): Deferred<R> =d

    override fun cancel() {
        d.cancel()
    }
}