package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.Job
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import kotlin.coroutines.experimental.CoroutineContext



class SagaProcessor<S>(
        private val dispatcherActor:SendChannel<Any>
)
{
    class Put(val value:Any)
    class TakeEvery<S,B>(val process: Saga2<S>.(B) -> Any?)
    class SagaFinished(val sm:SagaMiddleWare2<*>, val sagaName: String)

    /**
     * channel for communication between Saga and Saga processor
     */
    var channel :Channel<Any> = Channel()
    /**
     * channel where processor receive actions dispatched to the store
     */
    suspend fun start(inputActions:ReceiveChannel<Any>) {
        for(a in channel) {
            when(a) {
                is Put ->
                    dispatcherActor.send(a.value)
                is SagaFinished -> {
                    a.sm.sagaProcessorFinished(a.sagaName)
                    return
                }
                else ->  {
                    print("unsupported saga operation: ${a::class.java}")
                }
            }
        }
    }

    fun stop() {
        channel.close()
    }
}


class Saga2<S>(private val sagaProcessor:SagaProcessor<S>) {
    fun put(value:Any)= SagaProcessor.Put(value)
    fun <B> takeEvery(process: Saga2<S>.(B) -> Any?)= SagaProcessor.TakeEvery<S,B>(process)

    suspend fun yieldSingle(value: Any) {
        sagaProcessor.channel.send(value)
    }

    suspend fun yieldAll(inputChannel: ReceiveChannel<Any>) {
        for (a in inputChannel) {
            sagaProcessor.channel.send(a)
        }
    }
}

/**
 * a port of saga middleware
 * It store a (weak) reference to the store, so a distinct instance must be created for each store
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare2<S>(store_:Store<S>,val sagaContext:CoroutineContext= DefaultDispatcher) : Middleware<S> {
    private val dispatcherActor: SendChannel<Any>
    private val incomingActionsDistributionActor: SendChannel<Any>
    private val childSagas:MutableMap<String,SagaData<S>>
    private val store:WeakReference<Store<S>>
    init {
        store=WeakReference(store_)
        childSagas= mutableMapOf()
        //use an actor for dispatching so that we ensure we preserve action order
        dispatcherActor = actor<Any>(sagaContext) {
            for (a in channel) { //loop over incoming message
                store.get()?.dispatch?.invoke(a)
            }
        }
        //use an actor for distributing incoming actions so we ensure we preserve action order
        incomingActionsDistributionActor = actor<Any>(sagaContext) {
            for (a in channel) { // iterate over incoming actions
                //distribute incoming actions to sagas
                for (saga in childSagas.values) {
                    saga.inputActionsChannel?.send(a)
                }
            }
        }

    }
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        val res=nextDispatcher(action) //hit the reducers before processing actions in saga middleware!
        //use actor here to make sure that actions are distributed in the right order
        launch(sagaContext) {
            incomingActionsDistributionActor.send(action)
        }
        return res
    }

    private data class SagaData<S>(
            val inputActionsChannel: SendChannel<Any>?,
            val sagaProcessor:SagaProcessor<S>?,
            val sagaJob: Job?)

    fun runSaga(sagaName:String,sagafn: suspend Saga2<S>.() -> Unit) {

        val sagaProcessor=SagaProcessor<S>(dispatcherActor)
        val sagaInputActionsChannel=actor<Any>(sagaContext) {
            sagaProcessor.start(this)
        }

        val saga=Saga2(sagaProcessor)
        //start lazily, so that we have time to insert sagaData in childSagas map
        //because otherwise stopSaga() at the end of the sagaJob will not work
        val sagaJob= launch(sagaContext,start = CoroutineStart.LAZY) {
            saga.sagafn()
            launch(sagaContext){
                sagaFinished(sagaName)
            }
        }
        val sagaData=SagaData<S>(sagaInputActionsChannel,sagaProcessor,sagaJob)
        childSagas.put(sagaName,sagaData)
        //we are ready to start now
        sagaJob.start()
    }
    private suspend fun sagaFinished(sagaName: String) {
        childSagas.remove(sagaName)?.let { sagaData->
            sagaData.sagaProcessor?.channel?.send(SagaProcessor.SagaFinished(this, sagaName))
            childSagas.put(sagaName,sagaData.copy(sagaJob = null))
        }
    }
    internal fun sagaProcessorFinished(sagaName: String) {
        childSagas.remove(sagaName)?.let { sagaData->
            sagaData.inputActionsChannel?.close()
            childSagas.put(sagaName,sagaData.copy(sagaProcessor = null,inputActionsChannel = null))
        }
    }
    fun stopSaga(sagaName:String) {
        childSagas.remove(sagaName)?.apply {
            sagaJob?.cancel()
            inputActionsChannel?.close()
            sagaProcessor?.stop()
        }
    }



}
