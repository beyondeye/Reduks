package com.beyondeye.reduks.experimental.middlewares

import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.StoreSubscriberFn
import com.beyondeye.reduks.experimental.middlewares.saga.*
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlinx.coroutines.experimental.runBlocking
import org.assertj.core.api.Assertions.*
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

/**
 * Created by Dario on 17/12/2017.
 */
class SagaMiddleware2Test {
    sealed class SagaAction {
        class Plus(val value:Int) : SagaAction()
        class Minus(val value:Int) : SagaAction()
        class SetPlus(val value:Int):SagaAction()
        class SetMinus(val value:Int):SagaAction()
    }
    sealed class ActualAction {
        class IncrementCounter(val incrValue: Int):ActualAction()
        class DecrementCounter(val decrValue: Int):ActualAction()
        class SetIncrCounter(val incrValue: Int):ActualAction()
        class SetDecrCounter(val decrValue: Int):ActualAction()
        class EndAction:ActualAction()
    }
    data class TestState(val incrCounter: Int = 0,val decrCounter: Int = 0, val actionCounter: Int = 0, val endActionReceived: Boolean = false)

    val reducer = ReducerFn<TestState> { state, action ->
       when(action) {
           is ActualAction.DecrementCounter ->
                state.copy(decrCounter = state.decrCounter - action.decrValue, actionCounter = state.actionCounter + 1)
            is ActualAction.IncrementCounter ->
                state.copy(incrCounter = state.incrCounter + action.incrValue, actionCounter = state.actionCounter + 1)
            is ActualAction.SetIncrCounter ->
                state.copy(incrCounter = action.incrValue, actionCounter = state.actionCounter + 1)
            is ActualAction.SetDecrCounter ->
                state.copy(decrCounter = action.decrValue, actionCounter = state.actionCounter + 1)
            is ActualAction.EndAction ->
                state.copy(endActionReceived = true)
            else -> state
        }
    }


    @Test
    fun testSagaPut() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2(store)
        store.applyMiddleware(sagaMiddleware)
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==2) {
                            lock.countDown()
                        }
                    }
                })
        sagaMiddleware.runSaga("incr") {
            yield_ put ActualAction.IncrementCounter(123)
        }
        sagaMiddleware.runSaga("decr") {
            yield_ put ActualAction.DecrementCounter(321)
        }
        lock.await(5,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(2)
        assertThat(state.incrCounter).isEqualTo(123)
        assertThat(state.decrCounter).isEqualTo(-321)
//        store.dispatch(EndAction())
    }
    @Test
    fun testSagaTake() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            //wait for SagaAction.Plus type of action
            val a:SagaAction.Plus= yield_.take()
            yield_ put ActualAction.IncrementCounter(a.value)
        }
        sagaMiddleware.runSaga("decr") {
            //wait for SagaAction.Minus type of action
            val a:SagaAction.Minus= yield_.take()
            yield_ put ActualAction.DecrementCounter(a.value)
        }
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==2) {
                            lock.countDown()
                        }
                    }
                })
        //dispatch a SagaAction that will be translated to an actual action that the reducer can handle
        store.dispatch("some unhandled action")
        store.dispatch(SagaAction.Plus(123))
        store.dispatch(SagaAction.Minus(321))
        lock.await(50,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(2)
        assertThat(state.incrCounter).isEqualTo(123)
        assertThat(state.decrCounter).isEqualTo(-321)
//        store.dispatch(EndAction())
    }
    @Test
    fun testSagaDelay() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        val expectedDelay=1500L
        sagaMiddleware.runSaga("delay") {
            //wait for SagaAction.Plus type of action
            val actualDelay=measureTimeMillis {
                yield_ delay expectedDelay
            }
            yield_ put(ActualAction.SetIncrCounter(actualDelay.toInt()))
        }
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==1) {
                            lock.countDown()
                        }
                    }
                })
        lock.await(50,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(1)
        assertThat(state.incrCounter-expectedDelay).isLessThan(100)
//        store.dispatch(EndAction())
    }
    @Test
    fun testSagaCall() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)

        val totIncr=333
        val totDecr=-333
        val childSagaIncr=SagaFn1<TestState,Int,Int>("childSagaIncr") { p1->
            yield_ put(ActualAction.SetIncrCounter(p1))
            totIncr-p1
        }
        sagaMiddleware.runSaga("main") {
            val resIncr:Int= yield_ call childSagaIncr.withArgs(123)
            yield_ put ActualAction.IncrementCounter(resIncr)

            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yield_ put ActualAction.SetDecrCounter(p1)
                totDecr-p1
            }
            val resDecr:Int= yield_ call childSagaDecr.withArgs(-123)
            yield_ put ActualAction.DecrementCounter(-resDecr)

        }
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==4) {
                            lock.countDown()
                        }
                    }
                })
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(4)
        assertThat(state.incrCounter).isEqualTo(totIncr)
        assertThat(state.decrCounter).isEqualTo(totDecr)
    }
    @Test
    fun testSagaForkSpawnAndJoin() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)

        val totIncr=333
        val totDecr=-333
        val delayMs=1000L

        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==4) {
                            lock.countDown()
                        }
                    }
                })
        sagaMiddleware.runSaga("main") {
            val childSagaIncr= sagaFn("childSagaIncr") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.SetIncrCounter(p1)
                totIncr-p1
            }

            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yield_.delay(delayMs)
                yield_ put ActualAction.SetDecrCounter(p1)
                totDecr-p1
            }
            val incrForkTask = yield_ fork childSagaIncr.withArgs(123)
            val decrSpawnTask = yield_ spawn childSagaDecr.withArgs(-123)

            val resIncr= yield_ join incrForkTask
            yield_ put ActualAction.IncrementCounter(resIncr)
            val resDecr= yield_ join decrSpawnTask
            yield_ put ActualAction.DecrementCounter(-resDecr)

        }
        val actualExecTime= measureTimeMillis{
            lock.await(100,TimeUnit.SECONDS)
        }
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(4)
        assertThat(state.incrCounter).isEqualTo(totIncr)
        assertThat(state.decrCounter).isEqualTo(totDecr)
        //check that child sagas are executed in parallel
        assertThat(actualExecTime).isGreaterThan(delayMs)
        assertThat(actualExecTime).isLessThan(2*delayMs)
    }
    @Test
    fun testSagaForkSpawnAndCancelChildren() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)

        val canceledIncr=333
        val canceledDecr=333
        val actualIncr=123
        val actualDecr=123
        val delayMs=500L

        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==2) {
                            lock.countDown()
                        }
                    }
                })
        sagaMiddleware.runSaga("main") {
            val childSagaIncr= sagaFn("childSagaIncr") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.SetIncrCounter(p1)
            }
            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.SetDecrCounter(p1)
            }
            val incrForkTask = yield_ fork childSagaIncr.withArgs(canceledIncr)
            yield_ cancel incrForkTask //immediately cancel child tasks so that it does not dispatch any action
            val decrSpawnTask = yield_ spawn childSagaDecr.withArgs(canceledDecr)
            yield_ cancel decrSpawnTask //immediately cancel child tasks so that it does not dispatch any action
            yield_ delay  delayMs*2 //wait, to be sure that cancel actually blocked child task execution

            yield_ put(ActualAction.IncrementCounter(actualIncr))
            yield_ put(ActualAction.DecrementCounter(actualDecr))

        }
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(2)
        assertThat(state.incrCounter).isEqualTo(actualIncr)
        assertThat(state.decrCounter).isEqualTo(-actualDecr)
    }
    @Test
    fun testSagaSpawnForkParentCancel() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)

        val forkIncr=333
        val spawnDecr=-333
        val delayMs=500L

        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==1) {
                            lock.countDown()
                        }
                    }
                })
        sagaMiddleware.runSaga("main") {
            val childSagaIncr= sagaFn("childSagaIncr") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.SetIncrCounter(p1)
            }
            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.SetDecrCounter(p1)
            }
            val incrForkTask = yield_ fork childSagaIncr.withArgs(forkIncr)
            val decrSpawnTask = yield_ spawn childSagaDecr.withArgs(spawnDecr)
            //immediately cancel main saga
            yield_.cancelSelf()
        }
        //give time to make sure that cancellation actually happened
        runBlocking { delay(delayMs*2) }
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(1)
        //fork child Saga was cancelled by parent cancel!
        assertThat(state.incrCounter).isEqualTo(0)
        //spawn child Saga was not cancelled by parent cancel!
        assertThat(state.decrCounter).isEqualTo(spawnDecr)
    }
    fun TestSagaForkExceptionInParentTerminateChildren() {
        TODO()
    }
    @Test
    fun testSagaForkParentAutomaticallyWaitForChildCompletion() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)

        val forkIncr=333
        val delayMs=500L

        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==3) {
                            lock.countDown()
                        }
                    }
                })
        sagaMiddleware.runSaga("main") {
            val childSagaIncr2= sagaFn("childSagaIncr2") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.IncrementCounter(p1)
            }
            //childSagaDecr execution time should be equal the sum of its execution time and childSagaIncr,
            //because parent saga should automatically wait for children to complete
            val childSagaIncr1= sagaFn("childSagaIncr1") { p1:Int->
                yield_ delay delayMs
                yield_ put ActualAction.IncrementCounter(p1)
                val incr2Task = yield_ fork childSagaIncr2.withArgs(forkIncr)
            }
            val incr1Task = yield_ fork childSagaIncr1.withArgs(forkIncr)
            val runtime=measureTimeMillis {
                yield_ join incr1Task
            }
            yield_ put ActualAction.SetDecrCounter(runtime.toInt())

        }
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(3)
        val runtime = state.decrCounter
        //the combined runtime of childSagaIncr1 and childSagaIncr2
        assertThat(runtime).isGreaterThan(2*delayMs.toInt())
        assertThat(state.incrCounter).isEqualTo(2*forkIncr)
    }

    @Ignore
    @Test
    fun testSagaTakeEvery() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            yield_ takeEvery { a:SagaAction.Plus ->
                ActualAction.IncrementCounter(a.value)
            }
        }
        sagaMiddleware.runSaga("decr") {
            yield_ takeEvery { a:SagaAction.Minus ->
                ActualAction.DecrementCounter(a.value)
            }
        }
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter==2) {
                            lock.countDown()
                        }
                    }
                })
        store.dispatch(SagaAction.Plus(123))
        store.dispatch(SagaAction.Minus(321))
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(2)
        assertThat(state.incrCounter).isEqualTo(123)
        assertThat(state.decrCounter).isEqualTo(-321)
//        store.dispatch(EndAction())
    }
}




