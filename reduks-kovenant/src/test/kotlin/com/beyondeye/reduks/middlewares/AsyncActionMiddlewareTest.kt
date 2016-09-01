package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.KovenantStore
import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.StoreSubscriber
import org.assertj.core.api.Assertions
import org.junit.Test

/**
 * Created by Dario on 3/22/2016.
 */
class AsyncActionMiddlewareTest {
    class IncrementCounterAction
    class EndAction
    data class TestState(val counter:Int=0,val actionCounter:Int=0,val lastAsyncActionMessage: String = "none",val lastAsyncActionError: String? =null,
                         val lastAsyncActionResult:Int?=null,val lastAsyncActionResultString:String?=null,val endActionReceived:Boolean=false)
    val actionDifficultTag = "A very difficult mathematical problem"
    val actionDifficultTextTag = "A very difficult textual problem"
    val actionDifficultError ="Sometimes difficult problems cannot be solved"
    val reducer = Reducer<TestState> { state, action ->
        var res: TestState? = null
        AsyncAction.withPayload<Integer>( action)
                ?.onCompleted { payload ->
                    res = TestState(
                            actionCounter = state.actionCounter+1,
                            lastAsyncActionMessage = actionDifficultTag,
                            lastAsyncActionError = null,
                            lastAsyncActionResult = payload.toInt()
                    )
                }?.onFailed { error ->
                    res=TestState(
                            lastAsyncActionMessage = actionDifficultTag,
                             lastAsyncActionError = error.message,
                             lastAsyncActionResult = null)
        }
        AsyncAction.withPayload<String>( action)
                ?.onCompleted { payload ->
                    res = TestState(
                            actionCounter = state.actionCounter+1,
                            lastAsyncActionMessage = actionDifficultTextTag,
                            lastAsyncActionError = null,
                            lastAsyncActionResultString = payload
                    )
                }?.onFailed { error ->
                    res=TestState(
                        lastAsyncActionMessage = actionDifficultTextTag,
                        lastAsyncActionError = error.message,
                        lastAsyncActionResultString = null)
        }
        if(action is IncrementCounterAction)
            res=state.copy(counter = state.counter + 1,actionCounter = state.actionCounter+1)
        if(action is EndAction)
            res=state.copy(endActionReceived = true)
        res ?: state
    }
    @Test
    fun test_an_async_action_for_a_very_difficult_and_computation_heavy_operation() {
        val store = KovenantStore(TestState(), reducer,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())

        //subscribe before dispatch!!
        store.subscribe (StoreSubscriber {
            val state=store.state
            with (state) {
                if (lastAsyncActionMessage != "none") {
                    Assertions.assertThat(lastAsyncActionMessage).isEqualTo(actionDifficultTag)
                    Assertions.assertThat(lastAsyncActionError).isNull()
                    Assertions.assertThat(lastAsyncActionResult).isEqualTo(2 + 2)
                }
                if (endActionReceived) {
                    Assertions.assertThat(actionCounter).isEqualTo(1)
                }
            }
        }) //on state change
        val asyncAction = AsyncAction.start { 2 + 2 }
        store.dispatch(asyncAction)
        Thread.sleep(100) //wait for async action to be dispatched TODO: use thunk instead!!
        store.dispatch(EndAction())
    }
    @Test
    fun test_two_async_actions_with_different_payload_type() {
        val store = KovenantStore(TestState(), reducer,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())

        //subscribe before dispatch!!
        store.subscribe (StoreSubscriber {
                val state=store.state
                with (state) {
                    if (lastAsyncActionMessage == actionDifficultTag) {
                        Assertions.assertThat(lastAsyncActionError).isNull()
                        Assertions.assertThat(lastAsyncActionResult).isEqualTo(2 + 2)
                    }
                    if (lastAsyncActionMessage == actionDifficultTextTag) {
                        Assertions.assertThat(lastAsyncActionError).isNull()
                        Assertions.assertThat(lastAsyncActionResultString).isEqualTo("2 + 2")
                    }
                    if(endActionReceived) {
                        Assertions.assertThat(actionCounter).isEqualTo(2)
                    }
                }
        }) //on state change
        val asyncAction = AsyncAction.start  { 2 + 2 }
        store.dispatch(asyncAction)
        val asyncAction2 = AsyncAction.start { "2 + 2" }
        store.dispatch(asyncAction2)
        Thread.sleep(100) ////need to wait, because otherwise the end action will be dispatched before we complete the two async actions
        store.dispatch(EndAction())
    }

    @Test
    fun test_an_async_action_for_a_very_difficult_and_computation_heavy_operation_that_fails() {

        val store = KovenantStore(TestState(), reducer,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())

        //subscribe before dispatch!
        store.subscribe (
                StoreSubscriber {
                    val state=store.state
                    with(state) {
                        if (lastAsyncActionMessage != "none") {
                            Assertions.assertThat(lastAsyncActionMessage).isEqualTo(actionDifficultTag)
                            Assertions.assertThat(lastAsyncActionError).isEqualTo(actionDifficultError)
                            Assertions.assertThat(lastAsyncActionResult).isNull()
                        }
                        if(endActionReceived) {
                            Assertions.assertThat(actionCounter).isEqualTo(0) //one action failed
                        }
                    }
                }
        )
        val asyncAction = AsyncAction.start<Integer> {
            throw Exception(actionDifficultError)
        }
        store.dispatch(asyncAction)
        store.dispatch(EndAction())
    }
    @Test
    fun test_that_normal_actions_pass_through_the_middleware() {

        val store = KovenantStore(TestState(), reducer,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())


        //subscribe before dispatch!!
        store.subscribe (
                StoreSubscriber {
                    with(store.state) {
                        //on state change
                        Assertions.assertThat(counter).isEqualTo(1)
                        Assertions.assertThat(lastAsyncActionMessage).isEqualTo("none")
                        Assertions.assertThat(lastAsyncActionError).isNull()
                        Assertions.assertThat(lastAsyncActionResult).isNull()
                        if(endActionReceived) {
                            Assertions.assertThat(actionCounter).isEqualTo(1) //one action failed
                        }
                    }
                })
        store.dispatch(IncrementCounterAction())
        store.dispatch(EndAction())
    }
}

