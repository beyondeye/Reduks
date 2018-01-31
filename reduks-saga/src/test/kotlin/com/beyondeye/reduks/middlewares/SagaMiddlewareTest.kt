package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.SelectorBuilder
import com.beyondeye.reduks.StoreSubscriberFn
import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduks.experimental.middlewares.saga.*
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
class SagaMiddlewareTest {
    sealed class SagaAction {
        class Plus(val value:Int) : SagaAction()
        class Minus(val value:Int) : SagaAction()
        class SetPlus(val value:Int): SagaAction()
        class SetMinus(val value:Int): SagaAction()
    }
    sealed class ActualAction {
        class IncrementCounter(val incrValue: Int): ActualAction()
        class DecrementCounter(val decrValue: Int): ActualAction()
        class SetIncrCounter(val incrValue: Int): ActualAction()
        class SetDecrCounter(val decrValue: Int): ActualAction()
        class EndAction: ActualAction()
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
        val sagaMiddleware = SagaMiddleWare(store)
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
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            //wait for SagaAction.Plus type of action
            val a: SagaAction.Plus = yield_.take()
            yield_ put ActualAction.IncrementCounter(a.value)
        }
        sagaMiddleware.runSaga("decr") {
            //wait for SagaAction.Minus type of action
            val a: SagaAction.Minus = yield_.take()
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
    fun testSagaSelect() {
        val store = AsyncStore(TestState(incrCounter = 0,decrCounter = 0), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("select") {
            val selb=SelectorBuilder<TestState>()
            val selIncr = selb.withSingleField { incrCounter }
            val selDecr = selb.withSingleField { decrCounter }
            val initialIncrValue= yield_ select selIncr
            val initialDecrValue= yield_ select selDecr
            yield_ put ActualAction.SetIncrCounter(initialIncrValue-10)
            yield_ put ActualAction.SetDecrCounter(initialDecrValue+10)
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
        lock.await(50,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(2)
        assertThat(state.incrCounter).isEqualTo(-10)
        assertThat(state.decrCounter).isEqualTo(+10)
//        store.dispatch(EndAction())
    }

    @Test
    fun testSagaDelay() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
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
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
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
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
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
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
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
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
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
    @Test
    fun TestSagaForkExceptionInParentTerminateChildren() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
        store.applyMiddleware(sagaMiddleware)

        val forkIncr=333
        val spawnDecr=-333
        val delayMs=500L

        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        if (actionCounter>=1) {
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
            //immediately throw exception in main saga
            throw Exception("something went wrong in main saga! forked children should be automatically cancelled")
        }
        //give time to make sure that cancellation actually happened
        runBlocking { delay(delayMs*2) }
        lock.await(100,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(1)
        //fork child Saga was cancelled by parent exception!
        assertThat(state.incrCounter).isEqualTo(0)
        //spawn child Saga was not cancelled by parent exception!
        assertThat(state.decrCounter).isEqualTo(spawnDecr)
    }
    @Test
    fun testSagaForkParentAutomaticallyWaitForChildCompletion() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
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

    //this test currently fails
    @Ignore
    @Test
    fun testSagaTakeEvery() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        val initialDelaySecs:Double=0.1
        sagaMiddleware.runSaga("main") {
            val incr=sagaFn<Any>("setup_incr_filter") {
                yield_ takeEvery { a: SagaAction.Plus ->
                    yield_ put ActualAction.IncrementCounter(a.value)
                }
            }
            val j1= yield_ fork  incr
            val decr=sagaFn<Any>("setup_decr_filter") {
                yield_ takeEvery { a: SagaAction.Minus ->
                    yield_ put ActualAction.DecrementCounter(a.value)
                }
            }
            val j2= yield_ fork  decr
            //wait for setup up of takeEvery filter sagas
//            yield_ join listOf(j1,j2)
            //We should not get here!!! because child tasks of
            yield_ delay (initialDelaySecs*1000).toLong() //wait to make sure that incr and decr sagas started
            yield_ put SagaAction.Plus(123)
            yield_ put SagaAction.Minus(321)

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

        lock.await((initialDelaySecs+10).toLong(),TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isEqualTo(2)
        assertThat(state.incrCounter).isEqualTo(123)
        assertThat(state.decrCounter).isEqualTo(-321)
//        store.dispatch(EndAction())
    }
    //this test currently fails
    @Ignore
    @Test
    fun testSagaTakeLatest() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            yield_ takeLatest{ a:SagaAction.SetPlus ->
                yield_ put ActualAction.SetIncrCounter(a.value)
            }
        }
        sagaMiddleware.runSaga("decr") {
            yield_ takeLatest{ a:SagaAction.SetMinus ->
                yield_ put ActualAction.SetDecrCounter(a.value)
            }
        }
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        //                        if (actionCounter==2) {
//                            lock.countDown()
//                        }
                    }
                })
        val totrepsplus=100
        repeat(totrepsplus) {
            store.dispatch(SagaAction.SetPlus(123))
        }
        val totrepsminus=100
        repeat(totrepsminus) {
            store.dispatch(SagaAction.SetMinus(-321))
        }
        val totreps=totrepsplus+totrepsminus
        lock.await(2,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isLessThan(totreps) //take latest will conflate action of same type
        assertThat(state.incrCounter).isEqualTo(123) //one action failed
        assertThat(state.decrCounter).isEqualTo(-321) //one action failed
//        store.dispatch(EndAction())
    }
    //this test currently fails
    @Ignore
    @Test
    fun testSagaThrottle() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            yield_.throttle(100){ a:SagaAction.SetPlus ->
                yield_ put ActualAction.SetIncrCounter(a.value)
            }
        }
        sagaMiddleware.runSaga("decr") {
            yield_.throttle(100){ a:SagaAction.SetMinus ->
                yield_ put ActualAction.SetDecrCounter(a.value)
            }
        }
        val lock = CountDownLatch(1)

        store.subscribe(
                StoreSubscriberFn {
                    with(store.state) {
                        //                        if (actionCounter==2) {
//                            lock.countDown()
//                        }
                    }
                })
        val totrepsplus=100
        repeat(totrepsplus) {
            store.dispatch(SagaAction.SetPlus(123))
        }
        val totrepsminus=100
        repeat(totrepsminus) {
            store.dispatch(SagaAction.SetMinus(-321))
        }
        val totreps=totrepsplus+totrepsminus
        lock.await(2,TimeUnit.SECONDS)
        val state=store.state
        assertThat(state.actionCounter).isLessThan(totreps) //take latest will conflate action of same type
        assertThat(state.incrCounter).isEqualTo(123) //one action failed
        assertThat(state.decrCounter).isEqualTo(-321) //one action failed
//        store.dispatch(EndAction())
    }
}




