package com.beyondeye.reduks.experimental.middlewares.saga

import kotlinx.coroutines.experimental.cancel
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.delay
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.experimental.CoroutineContext

class SagaYeldSingle<S:Any>(private val sagaProcessor: SagaProcessor<S>){
    suspend infix fun put(value:Any) {
        _yieldSingle(OpCode.Put(value))
    }

    /**
     * yield a command to delay saga executing for the specified time.
     */
    suspend infix fun delay(timeMsecs: Long):Any {
        return _yieldSingle(OpCode.Delay(timeMsecs))
    }

    suspend infix fun <B> takeEvery(process: Saga2<S>.(B) -> Any?)
    {
        _yieldSingle( OpCode.TakeEvery<S,B>(process))
    }

    /**
     * yield a command to execute a child Saga in the same context of the current one (the parent saga): execution
     * of the parent saga is suspended until child saga completion. Exceptions in child saga execution
     * will bubble up to parent.
     * The parent saga and child saga share the same [SagaProcessor] and the same incoming actions channel.
     * In other words, incoming store actions processed in the child saga will be removed from the actions queue of parent saga
     */
    suspend infix fun <S:Any,R:Any> call(childSaga: SagaFn<S,R>):R {
        val res= _yieldSingle(OpCode.Call(childSaga))
        if(res is Throwable)
            throw res
        else
            @Suppress("UNCHECKED_CAST")
            return res as R
    }

    /**
     * join(task)
     * Creates an Effect description that instructs the middleware to wait for the result of a previously forked task.
     *
     * task: Task - A [SagaTask] object returned by a previous fork
     *
     * Notes
     *
     *      join will resolve to the same outcome of the joined task (success or error). If the joined task is cancelled,
     *
     * the cancellation will also propagate to the Saga executing the join effect.
     * Similarly, any potential callers of those joiners will be cancelled as well.
     */
    suspend  infix fun<R:Any> join( task:SagaTask<R>):R {
        val res=_yieldSingle(OpCode.JoinTasks(listOf(task)))
        if(res is Exception) throw res
        @Suppress("UNCHECKED_CAST")
        return (res as List<R>)[0]
    }
    /**
     * join(...tasks)
     *
     * Creates an Effect description that instructs the middleware to wait for the results of previously forked tasks.
     *
     *tasks: Array<[SagaTask]> - A Task is the object returned by a previous fork
     * Notes
     *   It simply wraps the array of tasks in join effects, roughly becoming the equivalent of yield tasks.map(t => join(t)).
     **/
    suspend infix fun join(tasks:List<SagaTask<Any>>):List<Any> {
       val res= _yieldSingle(OpCode.JoinTasks(tasks))
        if(res is Exception) throw res
        @Suppress("UNCHECKED_CAST")
        return res as List<Any>
    }
    //TODO write docs
    /**
     * Creates an Effect description that instructs the middleware to cancel a previously forked task.
     *
     * task: Task - A [SagaTask] object returned by a previous fork
     *
     * Notes
     *
     *      To cancel a running task, the middleware will invoke return on the underlying Generator object. This will cancel
     *      the current Effect in the task and jump to the finally block (if defined).
     *      Inside the finally block, you can execute any cleanup logic or dispatch some action to keep the store
     *      in a consistent state (e.g. reset the state of a spinner to false when an ajax request is cancelled).
     *      You can check inside the finally block if a Saga was cancelled by issuing a yield cancelled().
     *
     *      Cancellation propagates downward to child sagas. When cancelling a task, the middleware will also
     *      cancel the current Effect (where the task is currently blocked).
     *      If the current Effect is a call to another Saga, it will be also cancelled.
     *      When cancelling a Saga, all attached forks (sagas forked using yield fork()) will be cancelled.
     *      This means that cancellation effectively affects the whole execution tree that belongs to the cancelled task.
     *
     *      cancel is a non-blocking Effect. i.e. the Saga executing it will resume immediately after performing the cancellation.
     */
    suspend infix fun cancel( task:SagaTask<Any>) {
        _yieldSingle(OpCode.CancelTasks(listOf(task)))
    }
    suspend infix fun cancel( tasks:List<SagaTask<Any>>) {
        _yieldSingle(OpCode.CancelTasks(tasks))
    }
    //TODO write docs
    suspend fun cancel() {
        _yieldSingle(OpCode.CancelSelf())
    }
    /*
    cancelled()

        Creates an effect that instructs the middleware to return whether this generator has been cancelled. Typically
         you use this Effect in a finally block to run Cancellation specific code
Example


function* saga() {
  try {
    // ...
  } finally {
    if (yield cancelled()) {
      // logic that should execute only on Cancellation
    }
    // logic that should execute in all situations (e.g. closing a channel)
  }
}
     */
    //TODO write docs
    suspend fun cancelled():Boolean {
     //don't call processor
     //        _yieldSingle(OpCode.Cancelled())
        throw NotImplementedError("cancelled() not implemented: check instead if exception is of type CancellationException()")
    }

    /**
     * yield a command to execute a child Saga in a new context:  immediately return to execution
     * a [SagaTask]. The new saga has an independent incoming actions channel from the parent saga
     *
     */
    //TODO write docs
    /**
     *
     * All forked tasks are attached to their parents. When the parent terminates the execution of its own body of instructions, it will wait for all
     * forked tasks to terminate before returning.
     *    Errors from child tasks automatically bubble up to their parents. If any forked task raises an uncaught error,
     *    then the parent task will abort with the child Error, and the whole Parent's execution tree
     *    (i.e. forked tasks + the main task represented by the parent's body if it's still running) will be cancelled.
     * Cancellation of a forked Task will automatically cancel all forked tasks that are still executing. It'll also cancel
     * the current Effect where the cancelled task was blocked (if any).
     *  If a forked task fails synchronously (ie: fails immediately after its execution before performing any async operation),
     *  then no Task is returned, instead the parent will be aborted as soon as possible (since both parent and child executes
     *  in parallel, the parent will abort as soon as it takes notice of the child failure).
     */
    suspend infix fun <S:Any,R:Any> fork(childSaga: SagaFn<S,R>):SagaTask<R> {
        @Suppress("UNCHECKED_CAST")
        return _yieldSingle(OpCode.Fork(childSaga)) as SagaTask<R>
    }
    //TODO write docs
    /**
     * Same as fork(fn, ...args) but creates a detached task. A detached task remains independent from its parent and acts like a top-level task.
     * The parent will not wait for detached tasks to terminate before returning and all events which may affect the parent or the detached
     * task are completely independents (error, cancellation).
     */
    suspend infix fun <S:Any,R:Any> spawn(childSaga: SagaFn<S,R>):SagaTask<R> {
        @Suppress("UNCHECKED_CAST")
        return _yieldSingle(OpCode.Spawn(childSaga))as SagaTask<R>
    }

    //-----------------------------------------------
    suspend fun _yieldSingle(opcode: OpCode):Any {
        sagaProcessor.inChannel.send(opcode)
        if(opcode !is OpCode.OpCodeWithResult) return Unit
        return sagaProcessor.outChannel.receive()
    }
}
suspend inline fun <reified B> SagaYeldSingle<*>.take():B {
    return _yieldSingle(OpCode.Take<B>(B::class.java)) as B
}
//class SagaYeldAll<S:Any>(private val sagaProcessor: SagaProcessor<S>){
//    private suspend  fun yieldAll(inputChannel: ReceiveChannel<Any>) {
//        for (a in inputChannel) {
//            sagaProcessor.inChannel.send(a)
//        }
//    }
//}

class Saga2<S:Any>(sagaProcessor: SagaProcessor<S>) {
    val yieldSingle= SagaYeldSingle(sagaProcessor)
//    val yieldAll= SagaYeldAll(sagaProcessor)
    fun <R:Any> sagaFn(name: String, fn0: suspend Saga2<S>.() -> R) =
        SagaFn0(name, fn0)

    fun <P1, R : Any> sagaFn(name: String, fn1: suspend Saga2<S>.(p1: P1) -> R) =
            SagaFn1(name, fn1)
    fun <P1,P2,R:Any> sagaFn(name:String, fn2:suspend Saga2<S>.(p1:P1, p2:P2)->R)=
        SagaFn2(name,fn2)
    fun <P1,P2,P3,R:Any> sagaFn(name:String, fn3:suspend Saga2<S>.(p1:P1, p2:P2, p3:P3)->R)=
        SagaFn3(name,fn3)
}
sealed class OpCode {
    open class OpCodeWithResult:OpCode()
    class Delay(val time: Long,val unit: TimeUnit = TimeUnit.MILLISECONDS):OpCodeWithResult()
    class Put(val value:Any): OpCode()
    class Take<B>(val type:Class<B>): OpCodeWithResult()
    class TakeEvery<S:Any,B>(val process: Saga2<S>.(B) -> Any?): OpCode()
    class SagaFinished<R:Any>(val result: R, val isChildCall: Boolean): OpCode()
    class Call<S:Any,R:Any>(val childSaga: SagaFn<S,R>) : OpCodeWithResult()
    class Fork<S:Any,R:Any>(val childSaga: SagaFn<S,R>) : OpCodeWithResult()
    class Spawn<S:Any,R:Any>(val childSaga: SagaFn<S,R>) : OpCodeWithResult()
    class JoinTasks(val tasks:List<SagaTask<out Any>>): OpCodeWithResult()
    class CancelTasks(val tasks:List<SagaTask<out Any>>): OpCode()
    class CancelSelf: OpCode()
//    class Cancelled:OpCode()
}

class SagaProcessor<S:Any>(
        val sagaName:String,
        sagaMiddleWare:SagaMiddleWare2<S>,
        private val dispatcherActor: SendChannel<Any> = actor {  }
)
{
    internal var linkedSagaCoroutineContext: CoroutineContext?=null
    private var childCounter:Long=0
    private val sm=WeakReference(sagaMiddleWare)


    /**
     * channel for communication between Saga and Saga processor
     */
    val inChannel : Channel<Any> = Channel()
    val outChannel : Channel<Any> = Channel()
    /**
     * channel where processor receive actions dispatched to the store
     */
    suspend fun start(inputActions: ReceiveChannel<Any>) {
        try {
            processingLoop(inputActions)
        }
        catch (e:CancellationException) {
            sm.get()?.updataSagaDataAfterProcessorFinished(sagaName)
        }
        finally{

        }
    }

    private suspend fun processingLoop(inputActions: ReceiveChannel<Any>) {
        for(a in inChannel) {
            when(a) {
                is OpCode.Delay -> {
                    delay(a.time,a.unit)
                    outChannel.send(Unit)
                }
                is OpCode.Put ->
                    dispatcherActor.send(a.value)
                is OpCode.Take<*> -> {
//                    launch { //launch aynchronously, to avoid dead locks?
                    for(ia in inputActions) {
                        if(ia::class.java==a.type) {
                            outChannel.send(ia)
                            break
                        }
                    }
//                    }
                }
                is OpCode.Call<*, *> -> {
                    sm.get()?.let { sagaMiddleware->
                        val cs:SagaFn<S,Any> = a.childSaga as SagaFn<S,Any>
                        val childSagaName=buildChildSagaName("_call_",cs.name)
                        val childTask=sagaMiddleware._runSaga(cs,this,childSagaName,SagaProcessor.SAGATYPE_CHILD_CALL)
                    }
                }
                is OpCode.Fork<*, *> -> {
                    sm.get()?.let { sagaMiddleware->
                        val cs:SagaFn<S,Any> = a.childSaga as SagaFn<S,Any>
                        val childSagaName=buildChildSagaName("_fork_",cs.name)
                        val childTask=sagaMiddleware._runSaga(cs,this,childSagaName,SagaProcessor.SAGATYPE_CHILD_FORK)
                        outChannel.send(childTask)
                    }
                }
                is OpCode.Spawn<*, *> -> {
                    sm.get()?.let { sagaMiddleware->
                        val cs:SagaFn<S,Any> = a.childSaga as SagaFn<S,Any>
                        val childSagaName=buildChildSagaName("_spawn_",cs.name)
                        val childTask=sagaMiddleware._runSaga(cs,this,childSagaName,SagaProcessor.SAGATYPE_SPAWN)
                        outChannel.send(childTask)
                    }
                }
                is OpCode.SagaFinished<*> -> {
                    //add handling of result (if not unit) than need to back to parent saga (resolve task) promise
                    //also handling sending exception
                    outChannel.send(a.result) //if this is saga call that finished, don't stop processor!!
                    if(!a.isChildCall) {
                        sm.get()?.updataSagaDataAfterProcessorFinished(sagaName)
                        return
                    }
                }
                is OpCode.JoinTasks -> {
                    try {
                        val reslist:List<Any> = a.tasks.map { task ->
                            task.done().await()
                        }
                        outChannel.send(reslist)
                    } catch (e:Exception) {
                        outChannel.send(e)
                    }
                }
                is OpCode.CancelTasks -> {
                    a.tasks.forEach { it.cancel() }
                }
                is OpCode.CancelSelf -> {
                    linkedSagaCoroutineContext?.cancel()
                }
//                is OpCode.Cancelled -> {
//                    val res= sm?.get()?.isCancelled(sagaName) ?:false
//                    outChannel.send(res)
//                }
                else ->  {
                    print("unsupported saga operation: ${a::class.java}")
                }
            }
        }
    }

    private fun buildChildSagaName(prefix:String,name: String): String {
        if(name.isEmpty())
            return "$sagaName$prefix${childCounter++}"
        else
            return "$sagaName$prefix$name"
    }

    fun stop() {
        inChannel.close()
        outChannel.close()
    }
    companion object {
        /**
         * type of child saga: -call (sync): processor=parentProcessor
         */
        val SAGATYPE_CHILD_CALL =0
        /**
         * type of child saga:  -fork (async): new processor!=parentProcess,
         */
        val SAGATYPE_CHILD_FORK =1
        /**
         * type of child saga:  -spawn (async): like a totally separated saga (top level saga)
         */
        val SAGATYPE_SPAWN =2
    }
}