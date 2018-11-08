package com.beyondeye.reduks.experimental.middlewares.saga

import com.beyondeye.reduks.Selector
import com.beyondeye.reduks.SelectorBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.delay
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException

/**
 * extension function for yelding a state field: create a state selector on the fly, so you don't need to create it
 * by yourself if you just want to call yield_ selectField{<FieldName>}, instead of building a selector and calling
 * yield select <Selector>
 */
inline suspend infix fun <reified S:Any,I:Any> SagaYeldSingle<S>.selectField(noinline fieldSelector: S.() -> I):I {
    val selValue= SelectorBuilder<S>().withSingleField(fieldSelector)
    return this.select(selValue)
}

class SagaYeldSingle<S:Any>(private val sagaCmdProcessor: SagaCmdProcessor<S>){
    suspend infix fun put(value:Any) {
        _yieldSingle(OpCode.Put(value))
    }

    /**
     * yield a command to delay saga executing for the specified time.
     */
    suspend infix fun delay(timeMsecs: Long):Any {
        if(timeMsecs<=0)
            return Unit
        return _yieldSingle(OpCode.Delay(timeMsecs))
    }
    suspend infix  fun<O> select(selector:Selector<S,O>):O {
        @Suppress("UNCHECKED_CAST")
        return _yieldSingle(OpCode.Select(selector)) as O
    }

    /**
     * yield a command to execute a child Saga in the same context of the current one (the parent saga): execution
     * of the parent saga is suspended until child saga completion. Exceptions in child saga execution
     * will bubble up to parent.
     * The parent saga and child saga share the same [SagaCmdProcessor] and the same incoming actions channel.
     * In other words, incoming store actions processed in the child saga will be removed from the actions queue of parent saga
     */
    suspend infix fun <S:Any,R:Any> call(childSaga: SagaFn<S, R>):R {
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
    suspend  infix fun<R:Any> join( task: SagaTask<R>):R {
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
     *   It simply wraps the array of tasks in join effects, roughly becoming the equivalent of yield_ tasks.map(t => join(t)).
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
     *      You can check inside the finally block if a Saga was cancelled by issuing a yield_ cancelled().
     *
     *      Cancellation propagates downward to child sagas. When cancelling a task, the middleware will also
     *      cancel the current Effect (where the task is currently blocked).
     *      If the current Effect is a call to another Saga, it will be also cancelled.
     *      When cancelling a Saga, all attached forks (sagas forked using yield_ fork()) will be cancelled.
     *      This means that cancellation effectively affects the whole execution tree that belongs to the cancelled task.
     *
     *      cancel is a non-blocking Effect. i.e. the Saga executing it will resume immediately after performing the cancellation.
     */

    suspend infix fun<R:Any> cancel( task: SagaTask<R>) {
        _yieldSingle(OpCode.CancelTasks(listOf(task)))
    }
    suspend infix fun cancel( tasks:List<SagaTask<Any>>) {
        _yieldSingle(OpCode.CancelTasks(tasks))
    }
    //TODO write docs
    suspend fun cancelSelf() {
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
    if (yield_ cancelled()) {
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
     * yield_ a command to execute a child Saga in a new context:  immediately return to execution
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
    suspend infix fun <S:Any,R:Any> fork(childSaga: SagaFn<S, R>): SagaTask<R> {
        @Suppress("UNCHECKED_CAST")
        return _yieldSingle(OpCode.Fork(childSaga)) as SagaTask<R>
    }
    //TODO write docs
    /**
     * Same as fork(fn, ...args) but creates a detached task. A detached task remains independent from its parent and acts like a top-level task.
     * The parent will not wait for detached tasks to terminate before returning and all events which may affect the parent or the detached
     * task are completely independents (error, cancellation).
     */
    suspend infix fun <S:Any,R:Any> spawn(childSaga: SagaFn<S, R>): SagaTask<R> {
        @Suppress("UNCHECKED_CAST")
        return _yieldSingle(OpCode.Spawn(childSaga))as SagaTask<R>
    }

    //-----------------------------------------------
    suspend fun _yieldSingle(opcode: OpCode):Any {
        sagaCmdProcessor.inChannel.send(opcode)
        if(opcode !is OpCode.OpCodeWithResult) return Unit
        return sagaCmdProcessor.outChannel.receive()
    }
}
//-----------------------------------------------
internal suspend fun <B> SagaYeldSingle<*>.takeOfType(type:Class<B>):B {
    @Suppress("UNCHECKED_CAST")
    return _yieldSingle(OpCode.Take<B>(type)) as B
}
suspend inline fun <reified B> SagaYeldSingle<*>.take():B {
    return _yieldSingle(OpCode.Take<B>(B::class.java)) as B
}
//-----------------------------------------------
suspend inline infix fun <S:Any,reified B> SagaYeldSingle<S>.takeEvery(noinline handlerSaga:suspend Saga<S>.(p1:B)->Unit) {
    takeEvery(SagaFn1(B::class.java.simpleName,handlerSaga))
}
suspend inline infix fun <S:Any,reified B> SagaYeldSingle<S>.takeEvery(handlerSaga: SagaFn1<S, B, Unit>)
{
    _yieldSingle(OpCode.TakeEvery<S, B>(B::class.java,handlerSaga))
}
//-----------------------------------------------
suspend inline infix fun <S:Any,reified B> SagaYeldSingle<S>.takeLatest(noinline handlerSaga:suspend Saga<S>.(p1:B)->Unit) {
    takeLatest(SagaFn1(B::class.java.simpleName,handlerSaga))
}

suspend inline infix fun <S:Any,reified B> SagaYeldSingle<S>.takeLatest(handlerSaga: SagaFn1<S, B, Unit>)
{
    _yieldSingle(OpCode.TakeLatest<S, B>(B::class.java,handlerSaga))
}
//-----------------------------------------------
suspend inline fun <S:Any,reified B> SagaYeldSingle<S>.throttle(delayMs:Long,noinline handlerSaga:suspend Saga<S>.(p1:B)->Unit) {
    throttle(delayMs,SagaFn1(B::class.java.simpleName,handlerSaga))
}

suspend inline fun <S:Any,reified B> SagaYeldSingle<S>.throttle(delayMs:Long,handlerSaga:SagaFn1<S, B, Unit>)
{
    _yieldSingle(OpCode.Throttle<S, B>(B::class.java,delayMs,handlerSaga))
}
//-----------------------------------------------
class Saga<S:Any>(sagaCmdProcessor: SagaCmdProcessor<S>) {
    @JvmField val yield_ = SagaYeldSingle(sagaCmdProcessor)
//    val yieldAll= SagaYeldAll(sagaCmdProcessor)

    fun <R:Any> sagaFn(name: String, fn0: suspend Saga<S>.() -> R) =
        SagaFn0(name, fn0)

    fun <P1, R : Any> sagaFn(name: String, fn1: suspend Saga<S>.(p1: P1) -> R) =
            SagaFn1(name, fn1)
    fun <P1,P2,R:Any> sagaFn(name:String, fn2:suspend Saga<S>.(p1:P1, p2:P2)->R)=
            SagaFn2(name, fn2)
    fun <P1,P2,P3,R:Any> sagaFn(name:String, fn3:suspend Saga<S>.(p1:P1, p2:P2, p3:P3)->R)=
            SagaFn3(name, fn3)
}
sealed class OpCode {
    open class OpCodeWithResult: OpCode()
    abstract class FilterOpCode<S:Any>:OpCode() {
        abstract val sagaLabel:String
        abstract fun filterSaga(filterSagaName:String):SagaFn0<S,Unit>
    }
    class Delay(val timeMsecs: Long): OpCodeWithResult()
    class Put(val value:Any): OpCode()
    class Take<B>(val type:Class<B>): OpCodeWithResult()
    /**
     * Run  a child saga on each action dispatched to the Store that matches pattern.
     * takeEvery allows concurrent actions to be handled.
     * when a matching action is dispatched, a new handler task is started even
     * if a previous one is still pending (for example, the user clicks on a Load User button 2
     * consecutive times at a rapid rate, the 2nd click will dispatch a new action while the
     * previous task fired on the first one hasn't yet terminated)
     * takeEvery doesn't handle out of order responses from tasks. There is no guarantee that the tasks
     * will terminate in the same order they were started. To handle out of order responses,
     * you may consider [TakeLatest] below.
     */
    class TakeEvery<S:Any,B>(val type:Class<B>,val handlerSaga: SagaFn1<S, B, Unit>): FilterOpCode<S>()
    {
        override val sagaLabel="_tkevery_"
        //todo implement in an optimized way, not based on basic effects
        override  fun filterSaga(filterSagaName:String)= SagaFn0<S,Unit>(filterSagaName) {
            while (true) {
                val action: B = yield_.takeOfType(type)
                yield_ fork handlerSaga.withArgs(action)
            }
        }
    }

    /**
     * Run a child saga on each action dispatched to the Store that matches pattern.
     * And automatically cancels any previous saga task started previous if it's still running.
     */
    class TakeLatest<S:Any,B>(val type:Class<B>,val handlerSaga: SagaFn1<S, B, Unit>): FilterOpCode<S>()
    {
        override val sagaLabel="_tklatest_"
        override fun filterSaga(filterSagaName:String)= SagaFn0<S,Unit>(filterSagaName) {
            //todo implement in an optimized way, not based on basic effects
            var prevtask:SagaTask<Unit>?=null
            while (true) {
                val action: B = yield_.takeOfType(type)
                prevtask?.cancel() //if task already completed, this is a non-op
                prevtask=yield_ fork handlerSaga.withArgs(action)
            }
        }
    }

    /**
     * Run a child saga on an action dispatched to the Store that matches pattern.
     * After creating a child task it's still accepting incoming actions into the underlaying buffer, keeping at most 1
     * (the most recent one), but in the same time holding up with creating new task for ms milliseconds
     * (hence its name - throttle). Purpose of this is to ignore incoming actions for a given period of time
     * while processing a task.
     */
    class Throttle<S:Any,B>(val type:Class<B>,val delayMs:Long,val handlerSaga: SagaFn1<S, B, Unit>): FilterOpCode<S>()
    {
        override val sagaLabel="_throttle_"
        override fun filterSaga(filterSagaName:String)= SagaFn0<S,Unit>(filterSagaName) {
            //todo implement in an optimized way, not based on basic effects
            while (true) {
                val action: B = yield_.takeOfType(type)
                yield_ fork handlerSaga.withArgs(action)
                yield_ delay delayMs
            }
        }
    }
    class SagaFinished<R:Any>(val result: R, val isChildCall: Boolean): OpCode()
    class Call<S:Any,R:Any>(val childSaga: SagaFn<S, R>) : OpCodeWithResult()
    class Fork<S:Any,R:Any>(val childSaga: SagaFn<S, R>) : OpCodeWithResult()
    class Spawn<S:Any,R:Any>(val childSaga: SagaFn<S, R>) : OpCodeWithResult()
    class JoinTasks(val tasks:List<SagaTask<out Any>>): OpCodeWithResult()
    class CancelTasks(val tasks: List<SagaTask<out Any>>): OpCode()
    class CancelSelf: OpCode()
    class Select<S,O>(val selector: Selector<S,O>) :OpCodeWithResult()
    class Race :OpCodeWithResult()
    class All:OpCodeWithResult()
//    class Cancelled:OpCode()
}

class SagaCmdProcessor<S:Any>(
        val sagaName:String,
        sagaMiddleWare: SagaMiddleWare<S>,

        internal val linkedSagaParentScope: CoroutineScope,
        private val dispatcherActor: SendChannel<Any> = linkedSagaParentScope.actor {  }
)
{
    /**
     * this is needed when we want to execute the OpCode.CancelSelf,
     * this is job associated of the sagafn that is executed by the saga
     * that is started in [SagaMiddleWare._runSaga]
     */
    internal var linkedSagaJob: Job?=null
    private var childCounter:Long=0
    private val sm=WeakReference(sagaMiddleWare)


    /**
     * channel for communication between Saga and Saga processor
     * (RENDEZVOUS channel)
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
//        catch (e:Exception) {
//            print(e.message)
//        }
        finally{
            val a=1
        }
    }

    private suspend fun processingLoop(inputActions: ReceiveChannel<Any>) {
        for(a in inChannel) {
            when(a) {
                is OpCode.Delay -> {
                    delay(a.timeMsecs)
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
                is OpCode.Select<*,*> -> {
                    sm.get()?.store?.get()?.let { store ->
                        @Suppress("UNCHECKED_CAST")
                        val selector=a.selector as Selector<S,Any>
                        val res= selector(store.state)
                        outChannel.send(res)
                    }
                }
                is OpCode.Call<*, *> -> {
                    sm.get()?.let { sagaMiddleware->
                        @Suppress("UNCHECKED_CAST")
                        val cs: SagaFn<S, Any> = a.childSaga as SagaFn<S, Any>
                        val childSagaName=buildChildSagaName("_call_",cs.name)
                        val childTask=sagaMiddleware._runSaga(cs,this,childSagaName, SAGATYPE_CHILD_CALL)
                        //NOTE that here we are not sending the childTask to the outChannel (like in Fork, Spawn)
                        // this way, the main saga will be blocked waiting (yield) until the child saga complete and finally send its result to the outChannel
                    }
                }
                is OpCode.Fork<*, *> -> {
                    sm.get()?.let { sagaMiddleware->
                        @Suppress("UNCHECKED_CAST")
                        val cs: SagaFn<S, Any> = a.childSaga as SagaFn<S, Any>
                        val childSagaName=buildChildSagaName("_fork_",cs.name)
                        val childTask=sagaMiddleware._runSaga(cs,this,childSagaName, SAGATYPE_CHILD_FORK)
                        outChannel.send(childTask)
                    }
                }
                is OpCode.Spawn<*, *> -> {
                    sm.get()?.let { sagaMiddleware->
                        @Suppress("UNCHECKED_CAST")
                        val cs: SagaFn<S, Any> = a.childSaga as SagaFn<S, Any>
                        val childSagaName=buildChildSagaName("_spawn_",cs.name)
                        val childTask=sagaMiddleware._runSaga(cs,this,childSagaName, SAGATYPE_SPAWN)
                        outChannel.send(childTask)
                    }
                }
                is OpCode.SagaFinished<*> -> {
                    //add handling of result (if not unit) than need to back to parent saga (resolve task) promise
                    //also handling sending exception
                    if(a.isChildCall) {
                        //end of saga that was started with yield_ Call: return the result to the main saga as result of yield_ Call
                        outChannel.send(a.result) //if this is saga call that finished, don't stop processor!!
                    } else
                    {
                        //End of saga that was started with Spawn or Fork: no result need to be returned to the main saga:
                        //the main saga can monitor the child saga with the SagaTask that was returned on its start
                        sm.get()?.updataSagaDataAfterProcessorFinished(sagaName)
                        return //EXIT Command processor loop that basically kill the coroutine of the actor that is handling the sagaprocessor
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
                    linkedSagaJob?.cancel()
                }
//                is OpCode.Cancelled -> {
//                    val res= sm?.get()?.isCancelled(sagaName) ?:false
//                    outChannel.send(res)
//                }
                //TODO: refactor common code between TakeEvery/TakeLatest/Throttle
                //OpCode.TakeEvery<*,*>
                //OpCode.TakeLatest<*,*>
                //OpCode.Throttle<*,*>
                is OpCode.FilterOpCode<*> -> {
                    sm.get()?.let { sagaMiddleware ->
                        val filterSagaName = buildChildSagaName(a.sagaLabel, "")
                        @Suppress("UNCHECKED_CAST")
                        val fs = a.filterSaga(filterSagaName) as SagaFn0<S,Unit>
                        val childTask = sagaMiddleware._runSaga<Unit>(
                                fs,
                                this,
                                filterSagaName,
                                SAGATYPE_CHILD_FORK)
                    }
                }
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