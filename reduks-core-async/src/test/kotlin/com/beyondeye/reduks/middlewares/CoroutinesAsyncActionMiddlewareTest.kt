package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.AsyncStore
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.StoreSubscriberFn
import kotlinx.coroutines.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Created by Dario on 3/22/2016.
 */
class CoroutinesAsyncActionMiddlewareTest {
    class IncrementCounterAction
    class IncrementCounterIfPreviousMatching(val expectedPreviousCounterValue:Int)
    class EndAction
    data class TestState(val counter:Int=0,val actionCounter:Int=0,val lastAsyncActionMessage: String = "none",val lastAsyncActionError: String? =null,
                         val lastAsyncActionResult:Int?=null,val lastAsyncActionResultString:String?=null,val endActionReceived:Boolean=false)
    val actionDifficultTag = "A very difficult mathematical problem"
    val actionDifficultTextTag = "A very difficult textual problem"
    val actionDifficultError ="Sometimes difficult problems cannot be solved"
    val reducer = ReducerFn<TestState> { state, action ->
        var res: TestState? = null
        AsyncAction.withPayload<Int>(action)
                ?.onCompleted { payload ->
                    res = TestState(
                            actionCounter = state.actionCounter + 1,
                            lastAsyncActionMessage = actionDifficultTag,
                            lastAsyncActionError = null,
                            lastAsyncActionResult = payload
                    )
                }?.onFailed { error ->
                    res= TestState(
                            lastAsyncActionMessage = actionDifficultTag,
                            lastAsyncActionError = error.message,
                            lastAsyncActionResult = null)
        }
        AsyncAction.withPayload<String>(action)
                ?.onCompleted { payload ->
                    res = TestState(
                            actionCounter = state.actionCounter + 1,
                            lastAsyncActionMessage = actionDifficultTextTag,
                            lastAsyncActionError = null,
                            lastAsyncActionResultString = payload
                    )
                }?.onFailed { error ->
                    res= TestState(
                            lastAsyncActionMessage = actionDifficultTextTag,
                            lastAsyncActionError = error.message,
                            lastAsyncActionResultString = null)
        }
        if(action is IncrementCounterAction)
            res=state.copy(counter = state.counter + 1,actionCounter = state.actionCounter+1)
        if(action is EndAction)
            res=state.copy(endActionReceived = true)
        if(action is IncrementCounterIfPreviousMatching) {
            val curcounter=state.counter
            val newcounter=if(curcounter==action.expectedPreviousCounterValue) curcounter+1 else curcounter
            res=state.copy(counter=newcounter)
        }
        res ?: state
    }
    // TODO this tests sometimes (1 out of 10 times) fails: need to understand why and fix it
    @Test
    @Ignore
    fun test_an_async_action_for_a_very_difficult_and_computation_heavy_operation() {
        val store = AsyncStore(TestState(), reducer,GlobalScope,subscribeDispatcher = newSingleThreadContext("SubscribeThread")) //don't use android ui thread: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())
        val lock = CountDownLatch(1)
        //subscribe before dispatch!!
        store.subscribe (StoreSubscriberFn {
            val state=store.state
            with (state) {
                if (lastAsyncActionMessage != "none") {
                    assertThat(lastAsyncActionMessage).isEqualTo(actionDifficultTag)
                    assertThat(lastAsyncActionError).isNull()
                    assertThat(lastAsyncActionResult).isEqualTo(2 + 2)
                }
                if (endActionReceived) {
                    assertThat(actionCounter).isEqualTo(1)
                   lock.countDown() //release lock
                }
            }
        }) //on state change
        val asyncAction = AsyncAction.start(GlobalScope) { 2 + 2 }
        store.dispatch(asyncAction)
        runBlocking { delay(100) } //give time to the async action to complete: need to use coroutine delay method!!
        store.dispatch(EndAction())
        lock.await(2L,TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0L)
    }

    /**
     * this test is problematic: async action middleware will cause result of asyncactions to be dispatched in unpredictable order and delayed
     * with respect to action dispatched in the regular way (i.e. EndAction).
     * with the specific delays that I have added between dispatch of asyncAction1 and asyncAction2 and before dispatching EndAction()
     * this tests currently passes, but I am not 100% sure that I understand what is happening
     */
    @Test
    fun test_two_async_actions_with_different_payload_type() {
        val store = AsyncStore(TestState(), reducer,GlobalScope,subscribeDispatcher = newSingleThreadContext("SubscribeThread")) //false: otherwise exception if not running on android
        val asynmiddleware= AsyncActionMiddleWare<TestState>()
        store.applyMiddleware(asynmiddleware)
        val lock = CountDownLatch(1)
        //subscribe before dispatch!!
        store.subscribe (StoreSubscriberFn {
                val state=store.state
                with (state) {
                    if (lastAsyncActionMessage == actionDifficultTag) {
                        assertThat(lastAsyncActionError).isNull()
                        assertThat(lastAsyncActionResult).isEqualTo(2 + 2)
                    }
                    if (lastAsyncActionMessage == actionDifficultTextTag) {
                        assertThat(lastAsyncActionError).isNull()
                        assertThat(lastAsyncActionResultString).isEqualTo("2 + 2")
                    }
                    //TODO this throws an exception: also the exception is not shown when running tests: rewrite test (take advantage of the fact that now, after action dispatch, getting the state will block until reducer is run
                    if(endActionReceived) {
                        assertThat(actionCounter).isEqualTo(2)
                        lock.countDown() //release lock
                    }
                }
        }) //on state change
        val asyncAction = AsyncAction.start(GlobalScope) { 2 + 2 }
        store.dispatch(asyncAction)
        val d1=asynmiddleware.action_cscope.async { delay(100) } //give time to the two async actions to complete: need to use coroutine delay method!!
        runBlocking{d1.await() }
        val asyncAction2 = AsyncAction.start(GlobalScope) { "2 + 2" }
        store.dispatch(asyncAction2)
        val d2=asynmiddleware.action_cscope.async { delay(200) } //give time to the two async actions to complete: need to use coroutine delay method!!
        runBlocking{d2.await() }
        store.dispatch(EndAction())
        lock.await(5L,TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0L)
    }

    @Test
    fun test_an_async_action_for_a_very_difficult_and_computation_heavy_operation_that_fails() {

        val store = AsyncStore(TestState(), reducer,GlobalScope,subscribeDispatcher = newSingleThreadContext("SubscribeThread")) //custom subscribeDispatcher not UI: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())

        //subscribe before dispatch!
        store.subscribe (
                StoreSubscriberFn {
                    val state=store.state
                    with(state) {
                        if (lastAsyncActionMessage != "none") {
                            assertThat(lastAsyncActionMessage).isEqualTo(actionDifficultTag)
                            assertThat(lastAsyncActionError).isEqualTo(actionDifficultError)
                            assertThat(lastAsyncActionResult).isNull()
                        }
                        if(endActionReceived) {
                            assertThat(actionCounter).isEqualTo(0) //one action failed
                        }
                    }
                }
        )
        val asyncAction = AsyncAction.start<Int>(GlobalScope) {
            throw Exception(actionDifficultError)
        }
        store.dispatch(asyncAction)
        store.dispatch(EndAction())
    }
    @Test
    fun test_that_normal_actions_pass_through_the_middleware() {

        val store = AsyncStore(TestState(), reducer,GlobalScope,subscribeDispatcher = newSingleThreadContext("SubscribeThread")) //custom subscribeDispatcher not UI: otherwise exception if not running on android
        store.applyMiddleware(AsyncActionMiddleWare())


        //subscribe before dispatch!!
        store.subscribe (
                StoreSubscriberFn {
                    with(store.state) {
                        //on state change
                        assertThat(counter).isEqualTo(1)
                        assertThat(lastAsyncActionMessage).isEqualTo("none")
                        assertThat(lastAsyncActionError).isNull()
                        assertThat(lastAsyncActionResult).isNull()
                        if(endActionReceived) {
                            assertThat(actionCounter).isEqualTo(1) //one action failed
                        }
                    }
                })
        store.dispatch(IncrementCounterAction())
        store.dispatch(EndAction())
    }

    /**
     * NOTE that this test should be actually part of class AsyncStore test because it tests AsyncStore in general not  the AsyncActionMiddleware
     */
    @Test
    fun test_that_a_bunch_of_rapid_actions_are_all_executed_in_order() {

        val store = AsyncStore(TestState(), reducer,GlobalScope,subscribeDispatcher = newSingleThreadContext("SubscribeThread")) //custom subscribeDispatcher not UI: otherwise exception if not running on android
//        store.applyMiddleware(AsyncActionMiddleWare())
        val lock = CountDownLatch(1)
        val actionLoopMax=50000
        //subscribe before dispatch!!
        store.subscribe (
                StoreSubscriberFn {
                    val newState=store.state
                    with(newState) {
                        if(endActionReceived) {
                            assertThat(counter).isEqualTo(actionLoopMax) //one action failed
                            lock.countDown()
                        }
                    }
                })
        for(i in 1..actionLoopMax) {
            store.dispatch(IncrementCounterIfPreviousMatching(i-1))
        }
        store.dispatch(EndAction())
        lock.await(100,TimeUnit.SECONDS)
        assertThat(lock.count).isEqualTo(0)
    }
}

