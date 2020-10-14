package com.beyondeye.reduks.middlewares.saga

import com.beyondeye.reduks.*
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException

/**
 * a port of saga middleware
 * It store a (weak) reference to the store, so a distinct instance must be created for each store
 * https://github.com/redux-saga/redux-saga/
 * https://redux-saga.js.org/
 *
 * NOTE: [root_scope] is assumed be a [supervisorScope] so that if some its children fails, it is not automatically cancelled, see coroutine docs
 * Created by daely on 12/15/2017.
 */
class SagaMiddleWare<S:Any>(store_:Store<S>, parent_scope: CoroutineScope, val sagaMiddlewareDispatcher:CoroutineDispatcher= Dispatchers.Default) : Middleware<S> {
    private val dispatcherActor: SendChannel<Any>
    private val incomingActionsDistributionActor: SendChannel<Any>
    private var sagaMap:Map<String, SagaData<S, Any>>
    private val sagaMiddlewareRootJob:Job get() = sagaMiddlewareRootScope.coroutineContext[Job]!!
    private val sagaMiddlewareRootScope:CoroutineScope
    internal val store:WeakReference<Store<S>>
    init {
        //create a supervisor root job that is a child of parent_scope job for all coroutines started by the saga middleware
        //this is a job that IS NOT cancelled if one its children is cancelled
        //see https://github.com/Kotlin/kotlinx.coroutines/blob/master/docs/exception-handling.md#supervision
        val sagaMiddlewareRootJob=SupervisorJob(parent_scope.coroutineContext[Job])
        //create a root scope for all coroutine launched by Saga middleware, using the specified sagaMiddlewareDispatcher
        sagaMiddlewareRootScope=CoroutineScope(sagaMiddlewareDispatcher + sagaMiddlewareRootJob)
        store=WeakReference(store_)
        sagaMap = mapOf()
        //use an actor for dispatching so that we ensure we preserve action order
        dispatcherActor = sagaMiddlewareRootScope.actor<Any> {
            for (a in channel) { //loop over incoming message
                try { //don't let exception bubble up to sagas
                    store.get()?.dispatch?.invoke(a)
                } catch (e:Exception) {

                }
            }
        }
        //use an actor for distributing incoming actions so we ensure we preserve action order
        //define a channel with unlimited capacity, because we don't want the action dispatcher
        incomingActionsDistributionActor = sagaMiddlewareRootScope.actor<Any>(capacity = Channel.UNLIMITED) {
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
        sagaMiddlewareRootScope.launch {
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
        _runSaga(SagaFn0(sagaName, sagafn),null,sagaName, SagaCmdProcessor.SAGATYPE_SPAWN)
    }
    /**
     * replace an existing saga with the specified name. If a saga with the specified name exists and its is running, it wll be cancelled
     */
    fun replaceSaga(sagaName:String,sagafn: TopLevelSagaFn<S>) {
        if(sagaMap.containsKey(sagaName)) {
            stopSaga(sagaName)
        }
        _runSaga(SagaFn0(sagaName, sagafn),null,sagaName, SagaCmdProcessor.SAGATYPE_SPAWN)
    }

    /**
     * child saga type can be one of [SagaCmdProcessor.SAGATYPE_CHILD_CALL],[SagaCmdProcessor.SAGATYPE_CHILD_FORK],[SagaCmdProcessor.SAGATYPE_SPAWN]
     */
    internal fun <R:Any> _runSaga(sagafn: SagaFn<S, R>, parentSagaCmdProcessor: SagaCmdProcessor<S>?, sagaName: String, childType:Int): SagaTask<R> {

        if(childType!= SagaCmdProcessor.SAGATYPE_SPAWN &&parentSagaCmdProcessor==null)
            throw IllegalArgumentException("Only when spawning independent(top level) sagas parentSagaCmdProcessor can be null!")


        //
//        val parentSagaRootJob:Job
        val parentSagaScope:CoroutineScope
        if(parentSagaCmdProcessor==null) {
            parentSagaScope = sagaMiddlewareRootScope
        } else {
            parentSagaScope = CoroutineScope(parentSagaCmdProcessor.linkedSagaJob!!+sagaMiddlewareDispatcher)
        }
        val newSagaRootScope:CoroutineScope
        var newSagaInputActionsChannel: SendChannel<Any>?=null
        var newSagaCmdProcessor: SagaCmdProcessor<S>?=null
        when(childType) {
            /**
             * see [SagaYeldSingle.call]
             */
            SagaCmdProcessor.SAGATYPE_CHILD_CALL -> {
                newSagaRootScope = parentSagaScope
                newSagaInputActionsChannel = null //use parent saga action channel
                //if child call,  we are reusing the parent saga processor
                newSagaCmdProcessor = parentSagaCmdProcessor //use parent saga processor
            }
            /**
             * see [SagaYeldSingle.fork]
             */
            SagaCmdProcessor.SAGATYPE_CHILD_FORK -> {
                newSagaRootScope = parentSagaScope
                //------
                newSagaCmdProcessor = SagaCmdProcessor<S>(sagaName, this, newSagaRootScope,dispatcherActor)
                //define the saga processor receive channel, that is used to receive actions from dispatcher to have unlimited buffer, because we don't want to block the dispatcher
                newSagaInputActionsChannel = newSagaRootScope.actor<Any>(capacity = Channel.UNLIMITED) {
                    newSagaCmdProcessor!!.start(this)
                }
            }
            /**
             * see [SagaYeldSingle.spawn]
             */
            SagaCmdProcessor.SAGATYPE_SPAWN -> {
                newSagaRootScope = sagaMiddlewareRootScope
                //------
                newSagaCmdProcessor = SagaCmdProcessor<S>(sagaName, this, newSagaRootScope,dispatcherActor)
                //define the saga processor receive channel, that is used to receive actions from dispatcher to have unlimited buffer, because we don't want to block the dispatcher
                newSagaInputActionsChannel = newSagaRootScope.actor<Any>(capacity =  Channel.UNLIMITED) {
                    newSagaCmdProcessor.start(this)
                }
            }
            else -> throw NotImplementedError("This must not happen")
        }

        val newSaga = Saga(newSagaCmdProcessor!!)
        //start lazily, so that we have time to insert sagaData in sagaMap map
        //because otherwise stopSaga() at the end of the sagaJob will not work
        val sagaDeferred = newSagaRootScope.async( start = CoroutineStart.LAZY) {
            val isChildCall = (childType == SagaCmdProcessor.SAGATYPE_CHILD_CALL)
            val sagaResult = try {
                val res=sagafn.invoke(newSaga)
                //a parent coroutine will automatically wait for its children to complete execution, but
                //we want to handle this manually because we have to coordinate with saga associated sagaCmdProcessor
                val job=coroutineContext[Job]
                job?.children?.forEach { it.join() }
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
        //if the new saga is not a child saga, it has its new associated SagaCmdProcessor, so we need to
        //initialize the linkedSagaJob in the associated SagaCmdProcessor
        if (childType != SagaCmdProcessor.SAGATYPE_CHILD_CALL) {
            newSagaCmdProcessor.linkedSagaJob= sagaDeferred
        }

        val parentSagaName=when(childType) {
            SagaCmdProcessor.SAGATYPE_SPAWN -> "" //if spawn, don't return any result and don't register this saga as child
            else -> parentSagaCmdProcessor!!.sagaName
        }
        addSagaData(sagaName, SagaData(newSagaInputActionsChannel, newSagaCmdProcessor, sagaDeferred, parentSagaName))
        //we are ready to start now
        sagaDeferred.start()
        return SagaTaskFromDeferred(sagafn.name, sagaDeferred)
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
            sagaMiddlewareRootScope.launch {
                finishedSaga.sagaCmdProcessor?.inChannel?.send(OpCode.SagaFinished(result, isChildCall))
            }
            finishedSaga.copy(sagaJob = null,sagaJobResult = result)
        }
    }
    internal fun updataSagaDataAfterProcessorFinished(sagaName: String) {
        updateSagaData(sagaName) { sagaData ->
            if(sagaData==null) return@updateSagaData null
            sagaData.inputActionsChannel?.close()
            sagaData.copy(sagaCmdProcessor = null,inputActionsChannel = null)
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
                sagaCmdProcessor?.stop()
            }
        }
        val childSagasNames = getSagaChildrenNames(sagaName)
        childSagasNames.forEach { childSagaName->
            stopSaga(childSagaName)
        }
    }

    /**
     * completely stop all sagas and the saga middleware itself:
     * warning: after running [stopAll] the SagaMiddleware cannot be used any more
     * The result is `true` if the saga middleware was
     * cancelled as a result of this invocation and `false` if if it was already
     * cancelled or completed. See [Job.cancel] for details.
     */
    fun stopAll() {
        return sagaMiddlewareRootJob.cancel()
    }
    private fun getSagaChildrenNames(sagaName: String): List<String> {
        val childSagasNames = sagaMap.entries.filter { it.value.sagaParentName == sagaName }.map { it.key }
        return childSagasNames
    }
    private fun getSagaChildren(sagaName: String): List<SagaData<S, Any>> {
        val childSagas = sagaMap.entries.filter { it.value.sagaParentName == sagaName }.map { it.value }
        return childSagas
    }

    private fun updateSagaData(sagaName:String,updatefn:(SagaData<S, Any>?)-> SagaData<S, Any>?) {
        synchronized(this) {
            val curData= sagaMap[sagaName]
            val newSagaMap= sagaMap.toMutableMap()
            updatefn(curData)?.let{ newData->
                newSagaMap.put(sagaName,newData)
            }
            sagaMap =newSagaMap
        }
    }
    private fun addSagaData(sagaName:String,sagaData: SagaData<S, Any>) {
        synchronized(this) {
            val newSagaMap= sagaMap.toMutableMap()
            newSagaMap.put(sagaName,sagaData)
            sagaMap =newSagaMap
        }
    }
    private fun deleteSagaData(sagaName:String): SagaData<S, Any>? {
        var deletedSaga: SagaData<S, Any>?=null
        synchronized(this) {
            val newSagaMap= sagaMap.toMutableMap()
            deletedSaga=newSagaMap.remove(sagaName)
            sagaMap =newSagaMap
        }
        return deletedSaga
    }


}
