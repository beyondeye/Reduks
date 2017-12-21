package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import kotlin.coroutines.experimental.CoroutineContext

/**
 * a port of saga middleware
 * It store a (weak) reference to the store, so a distinct instance must be created for each store
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare2<S:Any>(store_:Store<S>,val rootSagaCoroutineContext:CoroutineContext= DefaultDispatcher) : Middleware<S> {
    private val dispatcherActor: SendChannel<Any>
    private val incomingActionsDistributionActor: SendChannel<Any>
    private var sagaMap:Map<String, SagaData<S,Any>>
    private val store:WeakReference<Store<S>>
    init {
        store=WeakReference(store_)
        sagaMap = mapOf()
        //use an actor for dispatching so that we ensure we preserve action order
        dispatcherActor = actor<Any>(rootSagaCoroutineContext) {
            for (a in channel) { //loop over incoming message
                try { //don't let exception bubble up to sagas
                    store.get()?.dispatch?.invoke(a)
                } catch (e:Exception) {

                }
            }
        }
        //use an actor for distributing incoming actions so we ensure we preserve action order
        //define a channel with unlimited capacity, because we don't want the action dispatcher
        incomingActionsDistributionActor = actor<Any>(rootSagaCoroutineContext,Channel.UNLIMITED) {
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
        launch(rootSagaCoroutineContext) {
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
    fun isCancelled(sagaName: String): Boolean {
        val sagaData= sagaMap[sagaName]
        if(sagaData==null) return true
        return sagaData.isCancelled()
    }


    /**
     * start a new saga with the specified name:
     * if you want to replace a saga that already exists, use [replaceSaga]. Trying to call [runSaga] with [sagaName] of
     * existing NOT COMPLETED saga will result in an exception
     */
    fun runSaga(sagaName:String,sagafn: TopLevelSagaFn<S>) {
        sagaMap[sagaName]?.let { existingSaga->
            if(!existingSaga.isCompleted())
                throw IllegalArgumentException("saga with name: $sagaName  already running: use replaceSaga() instead")
        }
        _runSaga(SagaFn0(sagaName,sagafn),null,sagaName,SagaProcessor.SAGATYPE_SPAWN)
    }
    /**
     * replace an existing saga with the specified name. If a saga with the specified name exists and its is running, it wll be cancelled
     */
    fun replaceSaga(sagaName:String,sagafn: TopLevelSagaFn<S>) {
        if(sagaMap.containsKey(sagaName)) {
            stopSaga(sagaName)
        }
        _runSaga(SagaFn0(sagaName,sagafn),null,sagaName,SagaProcessor.SAGATYPE_SPAWN)
    }

    /**
     * child saga type can be one of [SagaProcessor.SAGATYPE_CHILD_CALL],[SagaProcessor.SAGATYPE_CHILD_FORK],[SagaProcessor.SAGATYPE_SPAWN]
     */
    internal fun <R:Any> _runSaga(sagafn:SagaFn<S,R>, parentSagaProcessor:SagaProcessor<S>?, sagaName: String, childType:Int):SagaTask<R> {

        if(childType!=SagaProcessor.SAGATYPE_SPAWN &&parentSagaProcessor==null)
            throw IllegalArgumentException("Only when spawning independent(top level) sagas parentSagaProcessor can be null!")

        val parentSagaCoroutineContext = parentSagaProcessor?.linkedSagaCoroutineContext ?: rootSagaCoroutineContext
        val newSagaParentCoroutineContext = when (childType) {
            SagaProcessor.SAGATYPE_CHILD_CALL, SagaProcessor.SAGATYPE_CHILD_FORK -> parentSagaCoroutineContext
            SagaProcessor.SAGATYPE_SPAWN -> rootSagaCoroutineContext
            else -> rootSagaCoroutineContext
        }
        //-------
        var sagaInputActionsChannel: SendChannel<Any>?=null
        var sagaProcessor:SagaProcessor<S>?=null
        when(childType) {
            SagaProcessor.SAGATYPE_CHILD_CALL -> {
                sagaInputActionsChannel = null //use parent saga action channel
                sagaProcessor = parentSagaProcessor //use parent saga processor
            }
            SagaProcessor.SAGATYPE_CHILD_FORK, SagaProcessor.SAGATYPE_SPAWN -> {
                sagaProcessor = SagaProcessor<S>(sagaName, this, dispatcherActor)
                //define the saga processor receive channel, that is used to receive actions from dispatcher
                //to have unlimited buffer, because we don't want to block the dispatcher
                sagaInputActionsChannel = actor<Any>(rootSagaCoroutineContext, Channel.UNLIMITED) {
                    sagaProcessor.start(this)
                }
            }
        }

        val newSaga = Saga2(sagaProcessor!!)
        //start lazily, so that we have time to insert sagaData in sagaMap map
        //because otherwise stopSaga() at the end of the sagaJob will not work
        val sagaDeferred = async(newSagaParentCoroutineContext, start = CoroutineStart.LAZY) {
            val isChildCall = (childType == SagaProcessor.SAGATYPE_CHILD_CALL)
            if(!isChildCall) //if child call, don't reassign linked coroutine context, because we are reusing the parent saga processor
                sagaProcessor.linkedSagaCoroutineContext=coroutineContext
            val sagaResult = try {
                val res=sagafn.invoke(newSaga)
                //a parent coroutine will automatically wait for its children to complete execution, but
                //we want to handle this manually because we have coordinate with saga associated sagaProcessor
                coroutineContext[Job]?.children?.forEach { it.join() }
                res
            } catch (e: Throwable) {
                e
            }
            if (sagaResult is Throwable) {
                if(sagaResult is CancellationException) {
                    //need to cancel also associated sagaprocessor: it is an independent coroutine, not a child of the saga
                    //but child sagas are cancelled automatically because they are children coroutine of parent saga
                    sagaProcessorJob(sagaName)?.cancel()
                } else { // a normal exception: cancel children
                    coroutineContext.cancelChildren()
                }
            }

            sagaFinished(sagaName, sagaResult,isChildCall)
            return@async if (sagaResult is Throwable)
                throw sagaResult
            else
                @Suppress("UNCHECKED_CAST")
                sagaResult as R
        }
        val parentSagaName=when(childType) {
            SagaProcessor.SAGATYPE_SPAWN -> "" //if spawn, don't return any result and don't register this saga as child
            else -> parentSagaProcessor!!.sagaName
        }
        addSagaData(sagaName, SagaData(sagaInputActionsChannel, sagaProcessor, sagaDeferred, parentSagaName))
        //we are ready to start now
        sagaDeferred.start()
        return SagaTaskFromDeferred(sagafn.name,sagaDeferred)
    }

    private fun sagaProcessorJob(sagaName: String):Job? {
       return  sagaMap[sagaName]?.sagaProcessorJob()
    }

//    private suspend fun waitChildTasks(sagaName: String):List<Any> {
//        val childSagas = getSagaChildren(sagaName)
//        val reslist=childSagas.map { sagaData->
//            try {
//                sagaData.await()
//            } catch (e:Exception) {
//                e
//            }
//        }
//        return reslist
//    }

//    private suspend fun cancelChildTasks(sagaName: String) {
//        aa
//        val childSagas = getSagaChildren(sagaName)
//        childSagas.forEach { it.sagaJob?.cancel()  }
//    }


    private suspend fun <R:Any> sagaFinished(sagaName: String, result: R, isChildCall: Boolean) {
        updateSagaData(sagaName) { finishedSaga->
            if(finishedSaga==null)
                throw Exception("this must not happen")
            launch(rootSagaCoroutineContext) {
                finishedSaga.sagaProcessor?.inChannel?.send(OpCode.SagaFinished(result,isChildCall))
            }
            finishedSaga.copy(sagaJob = null,sagaJobResult = result)
        }
    }
    internal fun updataSagaDataAfterProcessorFinished(sagaName: String) {
        updateSagaData(sagaName) { sagaData ->
            if(sagaData==null) return@updateSagaData null
            sagaData.inputActionsChannel?.close()
            sagaData.copy(sagaProcessor = null,inputActionsChannel = null)
        }
    }
    /**
     * TODO: harmonize behavior of stopSaga with cancel() opcode used inside saga?
     */
    fun stopSaga(sagaName:String) {
        val deletedSaga=deleteSagaData(sagaName)
        deletedSaga?.apply {
            if(!isCompleted()) {
                sagaJob?.cancel()
                inputActionsChannel?.close()
                sagaProcessor?.stop()
            }
        }
        val childSagasNames = getSagaChildrenNames(sagaName)
        childSagasNames.forEach { childSagaName->
            stopSaga(childSagaName)
        }
    }

    private fun getSagaChildrenNames(sagaName: String): List<String> {
        val childSagasNames = sagaMap.entries.filter { it.value.sagaParentName == sagaName }.map { it.key }
        return childSagasNames
    }
    private fun getSagaChildren(sagaName: String): List<SagaData<S,Any>> {
        val childSagas = sagaMap.entries.filter { it.value.sagaParentName == sagaName }.map { it.value }
        return childSagas
    }

    private fun updateSagaData(sagaName:String,updatefn:(SagaData<S,Any>?)-> SagaData<S,Any>?) {
        synchronized(this) {
            val curData= sagaMap[sagaName]
            val newSagaMap= sagaMap.toMutableMap()
            updatefn(curData)?.let{ newData->
                newSagaMap.put(sagaName,newData)
            }
            sagaMap =newSagaMap
        }
    }
    private fun addSagaData(sagaName:String,sagaData: SagaData<S,Any>) {
        synchronized(this) {
            val newSagaMap= sagaMap.toMutableMap()
            newSagaMap.put(sagaName,sagaData)
            sagaMap =newSagaMap
        }
    }
    private fun deleteSagaData(sagaName:String): SagaData<S,Any>? {
        var deletedSaga: SagaData<S,Any>?=null
        synchronized(this) {
            val newSagaMap= sagaMap.toMutableMap()
            deletedSaga=newSagaMap.remove(sagaName)
            sagaMap =newSagaMap
        }
        return deletedSaga
    }


}
