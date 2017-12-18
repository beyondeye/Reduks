package com.beyondeye.reduks.experimental.middlewares

import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.StoreSubscriberFn
import com.beyondeye.reduks.experimental.middlewares.saga.*
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
            yieldSingle(put(ActualAction.IncrementCounter(123)))
        }
        sagaMiddleware.runSaga("decr") {
            yieldSingle(put(ActualAction.DecrementCounter(321)))
        }
        lock.await(1000,TimeUnit.SECONDS)
        val state=store.state
        Assertions.assertThat(state.actionCounter).isEqualTo(2) //one action failed
        Assertions.assertThat(state.incrCounter).isEqualTo(123) //one action failed
        Assertions.assertThat(state.decrCounter).isEqualTo(-321) //one action failed
//        store.dispatch(EndAction())
    }

    @Test
    fun testSagaTakeEvery() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare2<TestState>(store)
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga("incr") {
            yieldSingle(takeEvery<SagaAction.Plus> { a ->
                ActualAction.IncrementCounter(a.value)
            })
        }
        sagaMiddleware.runSaga("decr") {
            yieldSingle(takeEvery<SagaAction.Minus> { a ->
                ActualAction.DecrementCounter(a.value)
            })
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
        Assertions.assertThat(state.actionCounter).isEqualTo(2) //one action failed
        Assertions.assertThat(state.incrCounter).isEqualTo(123) //one action failed
        Assertions.assertThat(state.decrCounter).isEqualTo(-321) //one action failed
//        store.dispatch(EndAction())
    }
}


