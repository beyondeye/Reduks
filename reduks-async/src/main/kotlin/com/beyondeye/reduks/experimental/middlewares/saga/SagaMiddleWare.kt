package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

interface SagaScope<A>:ProducerScope<A> {
    val inputActions: Channel<A>
    suspend fun takeEvery(filter: (A) -> A?, process: (A) -> A?) = produce<A>(context) {
        for (a in inputActions) {
            filter(a)?.let { filtered -> process(filtered)?.let { send(it) } }
        }
    }
    suspend fun yield(value: A) {
        send(value)
    }
    suspend fun yieldAll(inputChannel:Channel<A>) {
        for (a in inputChannel) {
            send(a)
        }
    }
}

class Saga<S,E>(val context: CoroutineContext = DefaultDispatcher, inputActions:Channel<E>,outputChannel: Channel<E>, sagafn: suspend SagaScope<E>.() -> Unit) {
    val sagaCoroutine= produceToChannel<E>(context, inputActions, outputChannel, block = sagafn)
}


/*

class SagaAction
fun saga2(context: CoroutineContext, inputActions: ReceiveChannel<SagaAction>,filter:(Any)->Any?, process:(Any)->Any?) = produce<SagaAction>(context) {
    sendAll(takeEvery(context,inputActions,filter,process))
}

suspend fun sendAll(channel: ReceiveChannel<SagaAction>) {
    for (a in channel) {
        send(a)
    }
}
*/



/*

fun saga(context: CoroutineContext, inputActions: ReceiveChannel<SagaAction>, block:(Any)->Any?) = produce<SagaAction>(context) {
    for (a in inputActions)
    {
        block(a)?.let{send(it)}
    }
}
*/

/**
 * a port of saga middleware
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare<S>(val sagaContext:CoroutineContext= DefaultDispatcher) : Middleware<S> {
    private val inputActionsChannel:Channel<Any>
    private val processedActionsChannel:Channel<Any>
    private val dispatcherChannel: Job
    private val childSagas:MutableList<Saga<S, Any>>
    private var store:Store<S>?=null
    init {
        inputActionsChannel=Channel()
        processedActionsChannel = Channel()
        dispatcherChannel=launch(sagaContext) {
            for(a in processedActionsChannel) {
                store?.let { it.dispatch(a) }
            }
        }
        childSagas= mutableListOf()
    }
    fun runSaga(sagafn: suspend SagaScope<Any>.() -> Unit) {
        val newSaga= Saga<S, Any>(sagaContext, inputActionsChannel, processedActionsChannel, sagafn)
        childSagas.add(newSaga)
    }

    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        this.store=store
        launch(sagaContext) {
            inputActionsChannel.send(action)
        }
        return nextDispatcher(action)
    }

}
