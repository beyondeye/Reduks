package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.CoroutineStart
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.*
import kotlinx.coroutines.experimental.launch
import java.lang.ref.WeakReference
import kotlin.coroutines.experimental.CoroutineContext

/**
 * a port of saga middleware
 * It store a (weak) reference to the store, so a distinct instance must be created for each store
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare2<S:Any>(store_:Store<S>,val sagaContext:CoroutineContext= DefaultDispatcher) : Middleware<S> {
    private val dispatcherActor: SendChannel<Any>
    private val incomingActionsDistributionActor: SendChannel<Any>
    private var sagaMap:Map<String, SagaData<S>>
    private val store:WeakReference<Store<S>>
    init {
        store=WeakReference(store_)
        sagaMap = mapOf()
        //use an actor for dispatching so that we ensure we preserve action order
        dispatcherActor = actor<Any>(sagaContext) {
            for (a in channel) { //loop over incoming message
                try { //don't let exception bubble up to sagas
                    store.get()?.dispatch?.invoke(a)
                } catch (e:Exception) {

                }
            }
        }
        //use an actor for distributing incoming actions so we ensure we preserve action order
        //define a channel with unlimited capacity, because we don't want the action dispatcher
        incomingActionsDistributionActor = actor<Any>(sagaContext,Channel.UNLIMITED) {
            for (a in channel) { // iterate over incoming actions
                //distribute incoming actions to sagas
                for (saga in sagaMap.values) {
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

    /**
     * return true is saga with specified name does not exist or if it is completed
     */
    fun isCompleted(sagaName: String):Boolean
    {
        val sagaData= sagaMap[sagaName]
        if(sagaData==null) return true
        return sagaData.isCompleted()
    }


    /**
     * start a new saga with the specified name:
     * if you want to replace a saga that already exists, use [replaceSaga]. Trying to call [runSaga] with [sagaName] of
     * existing NOT COMPLETED saga will result in an exception
     */
    fun runSaga(sagaName:String,sagafn: suspend Saga2<S>.() -> Unit) {
        sagaMap[sagaName]?.let { existingSaga->
            if(!existingSaga.isCompleted())
                throw IllegalArgumentException("saga with name: $sagaName  already running: use replaceSaga() instead")
        }
        _runSaga(sagafn, sagaName)
    }
    /**
     * replace an existing saga with the specified name. If a saga with the specified name exists and its is running, it wll be cancelled
     */
    fun replaceSaga(sagaName:String,sagafn: suspend Saga2<S>.() -> Unit) {
        if(sagaMap.containsKey(sagaName)) {
            stopSaga(sagaName)
        }
        _runSaga(sagafn, sagaName)
    }

    private fun _runSaga(sagafn: suspend Saga2<S>.() -> Unit, sagaName: String) {
        val sagaProcessor = SagaProcessor<S>(dispatcherActor)
        //define the saga processor receive channel, that is used to receive actions from dispatcher
        //to have unlimited buffer, because we don't want to block the dispatcher
        val sagaInputActionsChannel = actor<Any>(sagaContext, Channel.UNLIMITED) {
            sagaProcessor.start(this)
        }

        val saga = Saga2(sagaProcessor)
        //start lazily, so that we have time to insert sagaData in sagaMap map
        //because otherwise stopSaga() at the end of the sagaJob will not work
        val sagaJob = launch(sagaContext, start = CoroutineStart.LAZY) {
            saga.sagafn()
            launch(sagaContext) {
                sagaFinished(sagaName)
            }
        }
        addSagaData(sagaName, SagaData(sagaInputActionsChannel, sagaProcessor, sagaJob, ""))
        //we are ready to start now
        sagaJob.start()
    }

    private suspend fun sagaFinished(sagaName: String) {
        updateSagaData(sagaName) { finishedSaga->
            if(finishedSaga==null)
                throw Exception("this must not happen")
            launch(sagaContext) {
                finishedSaga.sagaProcessor?.inChannel?.send(OpCode.SagaFinished(this@SagaMiddleWare2, sagaName))
            }
            finishedSaga.copy(sagaJob = null)
        }
    }
    internal fun sagaProcessorFinished(sagaName: String) {
        updateSagaData(sagaName) { sagaData ->
            if(sagaData==null) return@updateSagaData null
            sagaData.inputActionsChannel?.close()
            sagaData.copy(sagaProcessor = null,inputActionsChannel = null)
        }
    }
    fun stopSaga(sagaName:String) {
        val deletedSaga=deleteSagaData(sagaName)
        deletedSaga?.apply {
            if(!isCompleted()) {
                sagaJob?.cancel()
                inputActionsChannel?.close()
                sagaProcessor?.stop()
            }
        }
        val childSagasNames=sagaMap.entries.filter { it.value.sagaParentName==sagaName }.map { it.key }
        childSagasNames.forEach { childSagaName->
            stopSaga(childSagaName)
        }
    }
    private fun updateSagaData(sagaName:String,updatefn:(SagaData<S>?)-> SagaData<S>?) {
        synchronized(this) {
            val curData= sagaMap[sagaName]
            val newSagaMap= sagaMap.toMutableMap()
            updatefn(curData)?.let{ newData->
                newSagaMap.put(sagaName,newData)
            }
            sagaMap =newSagaMap
        }
    }
    private fun addSagaData(sagaName:String,sagaData: SagaData<S>) {
        synchronized(this) {
            val newSagaMap= sagaMap.toMutableMap()
            newSagaMap.put(sagaName,sagaData)
            sagaMap =newSagaMap
        }
    }
    private fun deleteSagaData(sagaName:String): SagaData<S>? {
        var deletedSaga: SagaData<S>?=null
        synchronized(this) {
            val newSagaMap= sagaMap.toMutableMap()
            deletedSaga=newSagaMap.remove(sagaName)
            sagaMap =newSagaMap
        }
        return deletedSaga
    }

}
