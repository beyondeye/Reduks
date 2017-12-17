package com.beyondeye.reduks.experimental.middlewares

import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.StoreSubscriberFn
import com.beyondeye.reduks.experimental.middlewares.saga.SagaMiddleWare
import com.beyondeye.reduks.experimental.middlewares.saga.takeEvery
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.experimental.newSingleThreadContext
import org.assertj.core.api.Assertions
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by Dario on 17/12/2017.
 */
class SagaMiddlewareTest {
    sealed class SagaAction {
        class Plus(val value:Int) : SagaAction()
        class Minus(val value:Int) : SagaAction()
    }
    sealed class ActualAction {
        class IncrementCounterAction(val incrValue: Int):ActualAction()
        class DecrementCounterAction(val decrValue: Int):ActualAction()
        class EndAction:ActualAction()
    }
    data class TestState(val incrCounter: Int = 0,val decrCounter: Int = 0, val actionCounter: Int = 0, val endActionReceived: Boolean = false)

    val reducer = ReducerFn<TestState> { state, action ->
        var res: TestState? = null
        if (action is ActualAction.DecrementCounterAction)
            res = state.copy(decrCounter = state.decrCounter - action.decrValue, actionCounter = state.actionCounter + 1)
        if (action is ActualAction.IncrementCounterAction)
            res = state.copy(incrCounter = state.incrCounter + action.incrValue, actionCounter = state.actionCounter + 1)
        if (action is ActualAction.EndAction)
            res = state.copy(endActionReceived = true)
        res ?: state
    }



    @Test
    fun testSaga1() {
        val store = AsyncStore(TestState(), reducer, subscribeContext = newSingleThreadContext("SubscribeThread")) //custom subscribeContext not UI: otherwise exception if not running on android
        val sagaMiddleware = SagaMiddleWare<TestState>()
        store.applyMiddleware(sagaMiddleware)
        sagaMiddleware.runSaga {
            yield(takeEvery<SagaAction.Plus>{ a ->
                ActualAction.IncrementCounterAction(a.value)
            })
        }
        sagaMiddleware.runSaga {
            yield(takeEvery<SagaAction.Minus>{ a ->
                ActualAction.DecrementCounterAction(a.value)
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


