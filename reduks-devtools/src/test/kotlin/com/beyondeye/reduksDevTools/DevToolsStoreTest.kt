package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.StoreSubscriber
import com.beyondeye.reduks.combineReducers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DevToolsStoreTest {
    data class TestState(val message: String = "initial state")
    data class TestAction(val type: String = "unknown")

    @Test
    fun when_an_action_is_fired_the_corresponding_reducer_should_be_called_and_update_the_state_of_the_application() {
        val reducer = Reducer<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "to invoke" -> TestState("reduced")
                    else -> state
                }
                else -> state
            }
        }

        val store = DevToolsStore(TestState(), reducer)

        store.dispatch(TestAction(type = "to invoke"))

        assertThat(store.state.message).isEqualTo("reduced")
    }

    @Test
    fun when_two_reducers_are_combined_and_a_series_of_actions_are_fired_the_correct_reducer_should_be_called() {
        val helloReducer1 = "helloReducer1"
        val helloReducer2 = "helloReducer2"

        val reducer1 = Reducer<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    helloReducer1 -> TestState("oh hai")
                    else -> state
                }
                else -> state
            }
        }

        val reducer2 = Reducer<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    helloReducer2 -> TestState("mark")
                    else -> state
                }
                else -> state
            }
        }

        val store = DevToolsStore(TestState(), combineReducers(reducer1, reducer2))

        store.dispatch(TestAction(type = helloReducer1))
        assertThat(store.state.message).isEqualTo("oh hai")
        store.dispatch(TestAction(type = helloReducer2))
        assertThat(store.state.message).isEqualTo("mark")
    }

    @Test
    fun subscribers_should_be_notified_when_the_state_changes() {
        val store = DevToolsStore(TestState(), Reducer<TestState> { state, action -> TestState() })
        var subscriber1Called = false
        var subscriber2Called = false

        store.subscribe(StoreSubscriber { subscriber1Called = true })
        store.subscribe (StoreSubscriber { subscriber2Called = true })

        store.dispatch(TestAction())

        assertThat(subscriber1Called).isTrue()
        assertThat(subscriber2Called).isTrue()
    }

    @Test
    fun the_store_should_not_notify_unsubscribed_objects() {
        val store = DevToolsStore(TestState(), Reducer<TestState> { state, action -> TestState() })
        var subscriber1Called = false
        var subscriber2Called = false

        store.subscribe (StoreSubscriber { subscriber1Called = true })
        val subscription = store.subscribe(StoreSubscriber { subscriber2Called = true })
        subscription.unsubscribe()

        store.dispatch(TestAction())

        assertThat(subscriber1Called).isTrue()
        assertThat(subscriber2Called).isFalse()
    }

    @Test
    fun store_should_pass_the_current_state_to_subscribers() {
        val reducer = Reducer<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "to invoke" -> TestState("oh hai")
                    else -> state
                }
                else -> state
            }
        }

        var actual: TestState = TestState()
        val store = DevToolsStore(TestState(), reducer)

        store.subscribe(StoreSubscriber { actual = it })
        store.dispatch(TestAction(type = "to invoke"))

        assertThat(actual).isEqualTo(store.state)
    }

    @Test
    fun store_should_work_with_both_dev_tools_actions_and_application_actions() {
        val reducer = Reducer<TestState> { state, action ->
            when (action) {
                is TestAction -> when (action.type) {
                    "to invoke" -> TestState("oh hai")
                    else -> state
                }
                else -> state
            }
        }

        val store = DevToolsStore(TestState(), reducer)

        store.dispatch(TestAction(type = "to invoke"))
        assertThat(store.state.message).isEqualTo("oh hai")

        store.dispatch(DevToolsAction.createResetAction())
        assertThat(store.state.message).isEqualTo(TestState().message)
    }
}

