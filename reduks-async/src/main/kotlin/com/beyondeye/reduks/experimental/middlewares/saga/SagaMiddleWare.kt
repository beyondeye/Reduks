package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

interface SagaScope<A> : ProducerScope<A> {
    val inputActions: Channel<A>

    fun takeEvery(filter: (A) -> A?, process: (A) -> A?) = produce<A>(coroutineContext) {
        for (a in inputActions) {
            filter(a)?.let { filtered -> process(filtered)?.let { send(it) } }
        }
    }

    suspend fun yieldSingle(value: A) {
        send(value)
    }

    suspend fun yield(inputChannel: ReceiveChannel<A>) {
        for (a in inputChannel) {
            send(a)
        }
    }
}

inline fun <reified B> SagaScope<Any>.takeEvery(crossinline process: (B) -> Any?) = produce<Any>(coroutineContext) {
    val actionType=B::class.java
    for (a in inputActions) {
        if (a::class.java == actionType) {
            process(a as B)?.let {
                send(it)
            }
        }
    }
}

//TODO refactor common code between takeEvery and takeLatest and throttle
inline fun <reified B> SagaScope<Any>.takeLatest(crossinline process: (B) -> Any?) = produce<Any>(coroutineContext,Channel.CONFLATED) {
    val actionType=B::class.java
    for (a in inputActions) {
        if (a::class.java == actionType) {
            process(a as B)?.let {
                send(it)
            }
        }
    }
}

//TODO refactor common code between takeEvery and takeLatest and throttle
inline fun <reified B> SagaScope<Any>.throttle(delayMs:Long,crossinline process: (B) -> Any?) = produce<Any>(coroutineContext,Channel.CONFLATED) {
    val actionType=B::class.java
    for (a in inputActions) {
        if (a::class.java == actionType) {
            process(a as B)?.let {
                send(it)
            }
            delay(delayMs)
        }
    }
}



class Saga<S,E>(context: CoroutineContext, val inputActions:Channel<E>,outputChannel: Channel<E>, sagafn: suspend SagaScope<E>.() -> Unit) {
    val sagaCoroutine= produceToChannel<E>(context, inputActions, outputChannel, block = sagafn)
}

/**
 * a port of saga middleware
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare<S>(val sagaContext:CoroutineContext= DefaultDispatcher) : Middleware<S> {
    private val processedActionsChannel:Channel<Any>
    private val dispatcherChannel: Job
    private val childSagas:MutableList<Saga<S, Any>>
    private var store:Store<S>?=null
    init {
        processedActionsChannel = Channel()
        dispatcherChannel = launch(sagaContext) {
            for (a in processedActionsChannel) {
                if (store == null) continue
                store!!.dispatch(a)
            }
        }
        childSagas= mutableListOf()
    }
    fun runSaga(sagafn: suspend SagaScope<Any>.() -> Unit) {
        val newSaga= Saga<S, Any>(sagaContext, Channel(), processedActionsChannel, sagafn)
        childSagas.add(newSaga)
    }

    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        this.store=store
        launch(sagaContext) {
            for(s in childSagas) {
                s.inputActions.send(action)
            }
        }
        return nextDispatcher(action)
    }

}
