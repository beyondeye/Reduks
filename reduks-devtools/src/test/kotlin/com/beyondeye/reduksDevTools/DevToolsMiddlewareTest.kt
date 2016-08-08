package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.Middleware
import com.beyondeye.reduks.Reducer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import rx.Observable
import rx.schedulers.TestScheduler
import java.util.concurrent.TimeUnit

class DevToolsMiddlewareTest {
    data class TestState(val message: String = "initial state")
    data class TestAction(val type: String = "unknown")
    object HeyHey
    object CallApi
    object Fetching
    object FetchComplete
    object Around

    @Test fun unwrapped_actions_should_be_run_through_a_stores_middleware() {
        var counter = 0

        val reducer = Reducer<TestState> { state, action ->
            state.copy("Reduced?")
        }

        val middleWare = Middleware<TestState> { store,  next,action ->
            counter += 1
            next.dispatch(action)
        }

        val store = DevToolsStore(TestState(), reducer, middleWare)

        store.dispatch(TestAction())

        assertThat(counter).isEqualTo(2)
        assertThat(store.state.message).isEqualTo("Reduced?")
    }

    @Test
    fun unwrapped_actions_should_pass_through_the_middleware_chain_in_the_correct_order() {
        var counter = 0
        val order = mutableListOf<String>()

        val middleWare1 = Middleware<TestState> { store, next, action ->
            counter += 1
            order.add("first")
            next.dispatch(action)
            order.add("third")
        }

        val middleWare2 = Middleware<TestState> { store, next, action ->
            counter += 1
            order.add("second")
            next.dispatch(action)
        }

        val reducer = Reducer<TestState> { state, action ->
            when (action) {
                is HeyHey -> TestState(message = "howdy!")
                else -> state
            }
        }

        val store = DevToolsStore(TestState(), reducer, middleWare1, middleWare2)

        store.dispatch(HeyHey)

        assertThat(store.state).isEqualTo(TestState("howdy!"))
        assertThat(counter).isEqualTo(4)
        assertThat(order).isEqualTo(listOf("first", "second", "third", "first", "second", "third"))
    }

    @Test
    fun async_middleware_should_be_able_to_dispatch_followup_unwrapped_actions_that_travel_through_the_remaining_middleware() {
        var counter = 0
        val order = mutableListOf<String>()
        val testScheduler = TestScheduler()

        val fetchMiddleware = Middleware<TestState> { store, next, action ->
            counter += 1
            when (action) {
                is CallApi -> {
                    next.dispatch(Fetching)
                    Observable
                            .just(5)
                            .delay(1L, TimeUnit.SECONDS, testScheduler)
                            .subscribe({
                                next.dispatch(FetchComplete)
                            })

                    next.dispatch(action)
                }
                else -> next.dispatch(action)
            }
        }

        val loggerMiddleware = Middleware<TestState> { store, next, action ->
            counter += 1
            when (action) {
                is CallApi -> order.add("CALL_API")
                is Fetching -> order.add("FETCHING")
                is FetchComplete -> order.add("FETCH_COMPLETE")
            }

            next.dispatch(action)
        }

        val reducer = Reducer<TestState> { state, action ->
            when (action) {
                Fetching -> TestState(message = "FETCHING")
                FetchComplete -> TestState(message = "FETCH_COMPLETE")
                else -> state
            }
        }

        val store = DevToolsStore(TestState(), reducer, fetchMiddleware, loggerMiddleware)

        store.dispatch(CallApi)

        assertThat(counter).isEqualTo(5)
        assertThat(order).isEqualTo(listOf("FETCHING", "CALL_API"))
        assertThat(store.state).isEqualTo(TestState("FETCHING"))

        testScheduler.advanceTimeBy(2L, TimeUnit.SECONDS)
        assertThat(counter).isEqualTo(6)
        assertThat(order).isEqualTo(listOf("FETCHING", "CALL_API", "FETCH_COMPLETE"))
        assertThat(store.state).isEqualTo(TestState(message = "FETCH_COMPLETE"))
    }

    @Test
    fun async_actions_should_be_able_to_send_new_unwrapped_actions_through_the_entire_chain() {
        var counter = 0
        val order = mutableListOf<String>()

        val middleWare1 = Middleware<TestState> { store, next, action ->
            counter += 1
            order.add("first")

            next.dispatch(action)

            // Redispatch an action that goes through the whole chain
            // (useful for async middleware)
            if (action is Around) {
                store.dispatch(TestAction())
            }
        }

        val middleWare2 = Middleware<TestState> { store, next, action ->
            counter += 1
            order.add("second")
            next.dispatch(action)
        }

        val reducer = Reducer<TestState> { state, action ->
            state
        }

        val store = DevToolsStore(TestState(), reducer, middleWare1, middleWare2)

        store.dispatch(Around)

        assertThat(counter).isEqualTo(6)
        assertThat(order).isEqualTo(listOf("first", "second", "first", "second", "first", "second"))
    }
}
