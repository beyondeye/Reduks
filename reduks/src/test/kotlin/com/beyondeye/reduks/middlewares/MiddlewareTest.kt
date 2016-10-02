package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import rx.Observable
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

class MiddlewareTest {
    data class TestState(val message: String = "initial state")
    data class TestAction(val type: String = "unknown")

    @Test
    fun actions_should_be_run_through_a_stores_middleware() {
        var counter = 0

        val reducer = ReducerFn<TestState> { state, action ->
            state
        }

        val middleWare = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            next(action)
        }

        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(middleWare)

        store.dispatch(TestAction(type = "hey hey!"))

        assertThat(store.state).isEqualTo(TestState())
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun actions_should_pass_through_the_middleware_chain_in_the_correct_order() {
        var counter = 0
        val order = mutableListOf<String>()

        val middleWare1 = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add("first")
            val nextAction = next(action)
            order.add("third")
        }

        val middleWare2 = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add("second")
            next(action)
        }

        val reducer = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "hey hey!" -> TestState(message = "howdy!")
                    else -> state
                }
                else -> state
            }
        }

        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(middleWare1, middleWare2)

        store.dispatch(TestAction(type = "hey hey!"))

        assertThat(store.state).isEqualTo(TestState("howdy!"))
        assertThat(counter).isEqualTo(2)
        assertThat(order).isEqualTo(listOf("first", "second", "third"))
    }

    @Test
    fun async_middleware_should_be_able_to_dispatch_followup_actions_that_travel_through_the_remaining_middleware() {
        var counter = 0
        val order = mutableListOf<String>()
        val testScheduler = TestScheduler()

        val fetchMiddleware = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            when (action) {
                is TestAction -> when (action.type) {
                    "CALL_API" -> {
                        next(TestAction("FETCHING"))
                        Observable
                                .just(5)
                                .delay(1L, TimeUnit.SECONDS, testScheduler)
                                .subscribe({
                                    next(TestAction("FETCH_COMPLETE"))
                                })

                        next(action)
                    }
                    else -> next(action)
                }
                else -> next(action)
            }
        }

        val loggerMiddleware = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add((action as TestAction).type)
            next(action)
        }

        val reducer = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "FETCHING" -> TestState(message = "FETCHING")
                    "FETCH_COMPLETE" -> TestState(message = "FETCH_COMPLETE")
                    else -> state
                }
                else -> state
            }
        }

        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(fetchMiddleware, loggerMiddleware)

        store.dispatch(TestAction(type = "CALL_API"))

        assertThat(counter).isEqualTo(3)
        assertThat(order).isEqualTo(listOf("FETCHING", "CALL_API"))
        assertThat(store.state).isEqualTo(TestState("FETCHING"))

        testScheduler.advanceTimeBy(2L, TimeUnit.SECONDS)
        assertThat(counter).isEqualTo(4)
        assertThat(order).isEqualTo(listOf("FETCHING", "CALL_API", "FETCH_COMPLETE"))
        assertThat(store.state).isEqualTo(TestState(message = "FETCH_COMPLETE"))
    }

    @Test
    fun async_actions_should_be_able_to_send_new_actions_through_the_entire_chain() {
        var counter = 0
        val order = mutableListOf<String>()

        val middleWare1 = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add("first")

            val nextAction = next(action)

            // Redispatch an action that goes through the whole chain
            // (useful for async middleware)
            if ((action as TestAction).type == "around!") {
                store.dispatch(TestAction())
            }
        }

        val middleWare2 = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add("second")
            next(action)
        }

        val reducer = ReducerFn<TestState> { state, action ->
            state
        }

        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(middleWare1, middleWare2)

        store.dispatch(TestAction(type = "around!"))

        assertThat(counter).isEqualTo(4)
        assertThat(order).isEqualTo(listOf("first", "second", "first", "second"))
    }

    @Test
    fun actions_should_be_run_through_a_stores_middleware_wrapped_as_store_enhancer() {
        var counter = 0

        val reducer = ReducerFn<TestState> { state, action ->
            state
        }

        val middleWare = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            next(action)
        }
        val enhancer=middleWare.toEnhancer()
        val store=SimpleStore.Creator<TestState>().create(reducer,TestState(),enhancer)

        store.dispatch(TestAction(type = "hey hey!"))

        assertThat(store.state).isEqualTo(TestState())
        assertThat(counter).isEqualTo(1)
    }

    @Test
    fun actions_should_pass_through_the_middleware_chain_in_the_correct_order_wrapped_as_store_enhancer() {
        var counter = 0
        val order = mutableListOf<String>()

        val middleWare1 = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add("first")
            val nextAction = next(action)
            order.add("third")
        }

        val middleWare2 = MiddlewareFn<TestState> { store, next, action ->
            counter += 1
            order.add("second")
            next(action)
        }


        val reducer = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "hey hey!" -> TestState(message = "howdy!")
                    else -> state
                }
                else -> state
            }
        }

        val enhancer1=middleWare1.toEnhancer()
        val enhancer2=middleWare2.toEnhancer()
        val store=SimpleStore.Creator<TestState>().create(reducer,TestState(),combineEnhancers(enhancer1,enhancer2))

        store.dispatch(TestAction(type = "hey hey!"))

        assertThat(store.state).isEqualTo(TestState("howdy!"))
        assertThat(counter).isEqualTo(2)
        assertThat(order).isEqualTo(listOf("first", "second", "third"))
    }
}
