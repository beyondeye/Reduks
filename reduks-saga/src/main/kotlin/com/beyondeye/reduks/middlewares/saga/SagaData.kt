package com.beyondeye.reduks.middlewares.saga

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel

/**
 * Created by daely on 12/19/2017.
 */
internal data class SagaData<S:Any,R:Any>(
        /**
         * note: this is actually the actor coroutine of the SagaCmdProcessor associated with the saga
         */
        val inputActionsChannel: SendChannel<Any>?,
        val sagaCmdProcessor: SagaCmdProcessor<S>?,
        val sagaJob: Deferred<R>?,
        val sagaParentName:String,
        val sagaJobResult:R?=null
        )
{
//    fun sagaProcessorCoroutine():AbstractCoroutine<Any> =inputActionsChannel as AbstractCoroutine<Any>
    fun sagaProcessorJob(): Job =inputActionsChannel as Job

    fun isCancelled() = if (sagaJob == null) sagaJobResult == null else sagaJob.isCancelled
    fun isCompleted() = if(sagaJob == null) sagaJobResult!=null else sagaJob.isCompleted
    suspend fun await():R {
        return sagaJobResult?:sagaJob?.await()!!
    }

}
