package com.beyondeye.reduks.experimental.middlewares.saga

import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.SendChannel

/**
 * Created by daely on 12/19/2017.
 */
data class SagaData<S:Any>(
        val inputActionsChannel: SendChannel<Any>?,
        val sagaProcessor: SagaProcessor<S>?,
        val sagaJob: Job?,
        val sagaParentName:String)
{
    fun isCompleted() = sagaJob == null
}
