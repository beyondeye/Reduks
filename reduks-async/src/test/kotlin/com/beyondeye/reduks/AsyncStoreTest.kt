package com.beyondeye.reduks

import com.beyondeye.reduks.experimental.AsyncStore
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AsyncStoreTest {
    data class TestState(val message: String = "initial state")
    data class TestAction(val type: String = "unknown")
    @Test
    fun when_an_action_is_fired_the_corresponding_reducer_should_be_called_and_update_the_state_of_the_application() {
        val reducer = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "to invoke" -> TestState("reduced")
                    else -> state
                }
                else -> state
            }
        }
        val lock = CountDownLatch(1)

        val store = AsyncStore(TestState(), reducer,observeOnUiThread = false) //false: otherwise exception if not running on android
        store.subscribe(StoreSubscriberFn {
            lock.countDown()
        })

        store.dispatch(TestAction(type = "to invoke"))
        lock.await(1000,TimeUnit.MILLISECONDS)
        assertThat(store.state.message).isEqualTo("reduced")
    }

    @Test
    fun when_two_reducers_are_combined_and_a_series_of_actions_are_fired_the_correct_reducer_should_be_called() {
        val helloReducer1 = "helloReducer1"
        val helloReducer2 = "helloReducer2"

        val reducer1 = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    helloReducer1 -> TestState("oh hai")
                    else -> state
                }
                else -> state
            }
        }

        val reducer2 = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    helloReducer2 -> TestState("mark")
                    else -> state
                }
                else -> state
            }
        }

        val lock = CountDownLatch(2)
        val store = AsyncStore(TestState(), combineReducers(reducer1, reducer2),observeOnUiThread = false) //false: otherwise exception if not running on android
        store.subscribe(StoreSubscriberFn {
            lock.countDown()
        })

        store.dispatch(TestAction(type = helloReducer1))
        lock.await(500,TimeUnit.MILLISECONDS)
        assertThat(store.state.message).isEqualTo("oh hai")
        //--------
        store.dispatch(TestAction(type = helloReducer2))
        lock.await(500,TimeUnit.MILLISECONDS)
        assertThat(store.state.message).isEqualTo("mark")
    }

    @Test
    fun subscribers_should_be_notified_when_the_state_changes() {
        val store = AsyncStore(TestState(), ReducerFn<TestState> { state, action -> TestState() },observeOnUiThread = false) //false: otherwise exception if not running on android
        var subscriber1Called = false
        var subscriber2Called = false
        val lock = CountDownLatch(2)

        store.subscribe(StoreSubscriberFn {
            subscriber1Called = true
            lock.countDown()
        })
        store.subscribe (StoreSubscriberFn {
            subscriber2Called = true
            lock.countDown()
        })

        store.dispatch(TestAction())

        lock.await(1000,TimeUnit.MILLISECONDS)

        assertThat(subscriber1Called).isTrue()
        assertThat(subscriber2Called).isTrue()
    }

    @Test
    fun the_store_should_not_notify_unsubscribed_objects() {
        val store = AsyncStore(TestState(), ReducerFn<TestState> { state, action -> TestState() },observeOnUiThread = false) //false: otherwise exception if not running on android
        var subscriber1Called = false
        var subscriber2Called = false

        val lock = CountDownLatch(2)

        store.subscribe (StoreSubscriberFn {
            subscriber1Called = true
            lock.countDown()
        })
        val subscription = store.subscribe(StoreSubscriberFn {
            subscriber2Called = true
            lock.countDown()
        })
        subscription.unsubscribe()

        store.dispatch(TestAction())

        lock.await(500,TimeUnit.MILLISECONDS)

        assert(lock.count==1L)
        assertThat(subscriber1Called).isTrue()
        assertThat(subscriber2Called).isFalse()
    }

    //TODO this tests sometimes will fail? fix it!!
    @Test
    fun store_should_pass_the_current_state_to_subscribers() {
        val reducer = ReducerFn<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "to invoke" -> TestState("oh hai")
                    else -> state
                }
                else -> state
            }
        }
        val lock = CountDownLatch(1)

        var actual: TestState = TestState()
        val store = AsyncStore(TestState(), reducer,observeOnUiThread = false) //false: otherwise exception if not running on android


        store.subscribe(StoreSubscriberFn {
            actual = store.state
            lock.countDown()
        }
        )
        store.dispatch(TestAction(type = "to invoke"))
        lock.await(1000,TimeUnit.MILLISECONDS)
        assertThat(store.state).isEqualTo(actual)
    }
}

