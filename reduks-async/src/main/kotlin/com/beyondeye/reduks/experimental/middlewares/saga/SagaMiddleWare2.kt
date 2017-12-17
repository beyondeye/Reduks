package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

interface SagaScope2<A> : ProducerScope<A> {
    val inputActions: Channel<A>

    class put(val value:Any)

    class takeEvery2<B>(val process: (B) -> Any?)
    suspend fun yieldSingle(value: A) {
        //TODO aa //make this two way comm??
        send(value)
    }

    suspend fun yieldAll(inputChannel: ReceiveChannel<A>) {
        //TOODO aa //make this two way comm ??
        for (a in inputChannel) {
            send(a)
        }
    }
}

class Saga2<S,E>(context: CoroutineContext, val inputActions:Channel<E>,outputChannel: Channel<E>, sagafn: suspend SagaScope2<E>.() -> Unit) {
    val sagaCoroutine= produceToChannel<E>(context, inputActions, outputChannel, block = sagafn)
}

/**
 * a port of saga middleware
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare2<S>(val sagaContext:CoroutineContext= DefaultDispatcher) : Middleware<S> {
    private val processedActionsChannel:Channel<Any>
    private val rootInputActionsChannel:Channel<Any>
    private val processorIncomingActionsJob: Job
    private val dispatcherJob: Job
    private val childSagas:MutableList<Saga2<S, Any>>
    //TODO store a weak reference to store?
    private var store:Store<S>?=null
    init {
        processedActionsChannel = Channel()
        rootInputActionsChannel= Channel()
        childSagas= mutableListOf()
        dispatcherJob = launch(sagaContext) {
            for (a in processedActionsChannel) {
                if (store == null) continue
                store!!.dispatch(a)
            }
        }
        processorIncomingActionsJob = launch(sagaContext) {
            for (a in rootInputActionsChannel) {
                for (saga in childSagas) { //TODO run a separate coroutine for each iteration in the loop of childSagas
                    saga.inputActions.send(a)
                }
            }
        }
    }
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        //TODO use weak reference to store?
        this.store=store
        val res=nextDispatcher(action) //hit the reducers before processing actions in saga middleware!
        launch(sagaContext) {
            rootInputActionsChannel.send(action)
        }
        return res
    }


    fun runSaga(sagafn: suspend SagaScope2<Any>.() -> Unit) {
        val newSaga= Saga2<S, Any>(sagaContext, Channel(), processedActionsChannel, sagafn)
        childSagas.add(newSaga)
        launch(sagaContext) {
            processActionCodes(newSaga.inputActions,newSaga.sagaCoroutine)
        }
        //processedActions.toChannel(processedActionsChannel)

    }
    private suspend fun processActionCodes(sagaInputChannel: Channel<Any>, sagaOutputActions: ReceiveChannel<Any>) {
        for(a in sagaOutputActions) {
            when(a) {
                is SagaScope2.put ->
                    processedActionsChannel.send(a.value)
                else ->  {
                    val s=a.toString()
                    print(s)
                }
            }
        }
    }


}
