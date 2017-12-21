package com.beyondeye.reduks.experimental.middlewares

import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.StoreSubscriberFn
import com.beyondeye.reduks.experimental.middlewares.saga.*
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.assertj.core.api.Assertions
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
            yieldSingle put ActualAction.IncrementCounter(123)
        }
        sagaMiddleware.runSaga("decr") {
            yieldSingle put ActualAction.DecrementCounter(321)
        }
        lock.await(5,TimeUnit.SECONDS)
        val state=store.state
        Assertions.assertThat(state.actionCounter).isEqualTo(2)
        Assertions.assertThat(state.incrCounter).isEqualTo(123)
        Assertions.assertThat(state.decrCounter).isEqualTo(-321)
//        store.dispatch(EndAction())
    }
    @Test
    fun testSagaTake() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            //wait for SagaAction.Plus type of action
            val a:SagaAction.Plus=yieldSingle.take()
            yieldSingle put ActualAction.IncrementCounter(a.value)
        }
        sagaMiddleware.runSaga("decr") {
            //wait for SagaAction.Minus type of action
            val a:SagaAction.Minus=yieldSingle.take()
            yieldSingle put ActualAction.DecrementCounter(a.value)
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
        Assertions.assertThat(state.actionCounter).isEqualTo(2)
        Assertions.assertThat(state.incrCounter).isEqualTo(123)
        Assertions.assertThat(state.decrCounter).isEqualTo(-321)
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
                yieldSingle delay expectedDelay
            }
            yieldSingle put(ActualAction.SetIncrCounter(actualDelay.toInt()))
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
        Assertions.assertThat(state.actionCounter).isEqualTo(1)
        Assertions.assertThat(state.incrCounter-expectedDelay).isLessThan(100)
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
            yieldSingle put(ActualAction.SetIncrCounter(p1))
            totIncr-p1
        }
        sagaMiddleware.runSaga("main") {
            val resIncr:Int=yieldSingle call childSagaIncr.withArgs(123)
            yieldSingle put ActualAction.IncrementCounter(resIncr)

            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yieldSingle put ActualAction.SetDecrCounter(p1)
                totDecr-p1
            }
            val resDecr:Int=yieldSingle call childSagaDecr.withArgs(-123)
            yieldSingle put ActualAction.DecrementCounter(-resDecr)

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
        Assertions.assertThat(state.actionCounter).isEqualTo(4)
        Assertions.assertThat(state.incrCounter).isEqualTo(totIncr)
        Assertions.assertThat(state.decrCounter).isEqualTo(totDecr)
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
                yieldSingle delay delayMs
                yieldSingle put ActualAction.SetIncrCounter(p1)
                totIncr-p1
            }

            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yieldSingle.delay(delayMs)
                yieldSingle put ActualAction.SetDecrCounter(p1)
                totDecr-p1
            }
            val incrForkTask = yieldSingle fork childSagaIncr.withArgs(123)
            val decrSpawnTask = yieldSingle spawn childSagaDecr.withArgs(-123)

            val resIncr=yieldSingle join incrForkTask
            yieldSingle put ActualAction.IncrementCounter(resIncr)
            val resDecr=yieldSingle join decrSpawnTask
            yieldSingle put ActualAction.DecrementCounter(-resDecr)

        }
        val actualExecTime= measureTimeMillis{
            lock.await(100,TimeUnit.SECONDS)
        }
        val state=store.state
        Assertions.assertThat(state.actionCounter).isEqualTo(4)
        Assertions.assertThat(state.incrCounter).isEqualTo(totIncr)
        Assertions.assertThat(state.decrCounter).isEqualTo(totDecr)
        //check that child sagas are executed in parallel
        Assertions.assertThat(actualExecTime).isGreaterThan(delayMs)
        Assertions.assertThat(actualExecTime).isLessThan(2*delayMs)
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
                yieldSingle delay delayMs
                yieldSingle put ActualAction.SetIncrCounter(p1)
            }
            val childSagaDecr= sagaFn("childSagaDecr") { p1:Int->
                yieldSingle delay delayMs
                yieldSingle put ActualAction.SetDecrCounter(p1)
            }
            val incrForkTask = yieldSingle fork childSagaIncr.withArgs(canceledIncr)
            yieldSingle  cancel incrForkTask //immediately cancel child tasks so that it does not dispatch any action
            val decrSpawnTask = yieldSingle spawn childSagaDecr.withArgs(canceledDecr)
            yieldSingle  cancel decrSpawnTask //immediately cancel child tasks so that it does not dispatch any action
            yieldSingle delay  delayMs*2 //wait, to be sure that cancel actually blocked child task execution

            yieldSingle put(ActualAction.IncrementCounter(actualIncr))
            yieldSingle put(ActualAction.DecrementCounter(actualDecr))

        }
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        Assertions.assertThat(state.actionCounter).isEqualTo(2)
        Assertions.assertThat(state.incrCounter).isEqualTo(actualIncr)
        Assertions.assertThat(state.decrCounter).isEqualTo(-actualDecr)
    }
    @Ignore
    @Test
    fun testSagaTakeEvery() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            yieldSingle takeEvery { a:SagaAction.Plus ->
                ActualAction.IncrementCounter(a.value)
            }
        }
        sagaMiddleware.runSaga("decr") {
            yieldSingle takeEvery { a:SagaAction.Minus ->
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
        Assertions.assertThat(state.actionCounter).isEqualTo(2)
        Assertions.assertThat(state.incrCounter).isEqualTo(123)
        Assertions.assertThat(state.decrCounter).isEqualTo(-321)
//        store.dispatch(EndAction())
    }
}




