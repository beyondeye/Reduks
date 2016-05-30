package com.beyondeye.reduksDevTools

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class DevToolsReducerTest {
    data class TestState(val message: String = "initial state")
    data class TestAction(val message: String = "test action")

    val testReducer = DevToolsTestReducer()

    @Test
    fun perform_action_should_update_the_dev_tools_store() {
        val store = DevToolsStore<TestState>(TestState(), testReducer)
        val message = "test"

        store.dispatch(TestAction(message))

        assertThat(store.devToolsState.committedState).isEqualTo(TestState())
        assertThat(store.devToolsState.computedStates.size).isEqualTo(2)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(2)
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState(message))
        assertThat(store.devToolsState.currentAction).isEqualTo(TestAction(message))
    }

    @Test
    fun when_back_in_time_the_perform_action_should_overwrite_all_future_actions() {
        val store = DevToolsStore<TestState>(TestState(), testReducer)
        val first = "first"
        val second = "second"
        val third = "third"

        store.dispatch(TestAction(first))
        store.dispatch(TestAction(second))
        store.dispatch(TestAction(third))

        assertThat(store.devToolsState.committedState).isEqualTo(TestState())
        assertThat(store.devToolsState.computedStates.size).isEqualTo(4)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(4)
        assertThat(store.devToolsState.computedStates[1]).isEqualTo(TestState(first))
        assertThat(store.devToolsState.stagedActions[1]).isEqualTo(TestAction(first))
        assertThat(store.devToolsState.computedStates[2]).isEqualTo(TestState(second))
        assertThat(store.devToolsState.stagedActions[2]).isEqualTo(TestAction(second))
        assertThat(store.devToolsState.computedStates[3]).isEqualTo(TestState(third))
        assertThat(store.devToolsState.stagedActions[3]).isEqualTo(TestAction(third))
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState(third))
        assertThat(store.devToolsState.currentAction).isEqualTo(TestAction(third))

        store.dispatch(DevToolsAction.createJumpToStateAction(2))
        store.dispatch(TestAction(first))

        assertThat(store.devToolsState.committedState).isEqualTo(TestState())
        assertThat(store.devToolsState.computedStates.size).isEqualTo(4)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(4)
        assertThat(store.devToolsState.computedStates[1]).isEqualTo(TestState(first))
        assertThat(store.devToolsState.stagedActions[1]).isEqualTo(TestAction(first))
        assertThat(store.devToolsState.computedStates[2]).isEqualTo(TestState(second))
        assertThat(store.devToolsState.stagedActions[2]).isEqualTo(TestAction(second))
        assertThat(store.devToolsState.computedStates[3]).isEqualTo(TestState(first))
        assertThat(store.devToolsState.stagedActions[3]).isEqualTo(TestAction(first))
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState(first))
        assertThat(store.devToolsState.currentAction).isEqualTo(TestAction(first))
    }

    @Test
    fun reset_action_should_roll_the_current_state_of_the_app_back_to_the_previously_saved_state() {
        val store = DevToolsStore<TestState>(TestState(), testReducer)

        store.dispatch(TestAction("action that will be lost when store is reset"))
        store.dispatch(DevToolsAction.createResetAction())

        assertThat(store.devToolsState.committedState).isEqualTo(TestState())
        assertThat(store.devToolsState.computedStates.size).isEqualTo(1)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(1)
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState())
        assertThat(store.devToolsState.currentAction).isEqualTo(DevToolsAction.createResetAction())
    }

    @Test
    fun save_action_should_commit_the_current_state_of_the_app() {
        val store = DevToolsStore<TestState>(TestState(), testReducer)
        val message = "action to save"

        store.dispatch(TestAction(message))
        store.dispatch(DevToolsAction.createSaveAction())

        assertThat(store.devToolsState.committedState).isEqualTo(TestState(message))
        assertThat(store.devToolsState.computedStates.size).isEqualTo(1)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(1)
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState(message))
        assertThat(store.devToolsState.currentAction).isEqualTo(DevToolsAction.createSaveAction())
    }

    @Test
    fun jump_to_state_action_should_set_the_current_state_of_the_app_to_a_given_time_in_the_past() {
        val store = DevToolsStore<TestState>(TestState(), testReducer)
        val jumpToMessage = "action to jump to"
        val finalMessage = "final action"

        store.dispatch(TestAction(jumpToMessage))
        store.dispatch(TestAction(finalMessage))
        store.dispatch(DevToolsAction.createJumpToStateAction(1))

        assertThat(store.devToolsState.computedStates.size).isEqualTo(3)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(3)
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState(jumpToMessage))
        assertThat(store.devToolsState.currentAction).isEqualTo(TestAction(jumpToMessage))
    }

    @Test
    fun recompute_action_should_run_all_actions_through_the_app_reducer_again() {
        val recomputeTestReducer = DevToolsTestReducer()
        val store = DevToolsStore<TestState>(TestState(), recomputeTestReducer)
        val first = "first"
        val second = "second"

        store.dispatch(TestAction(first))
        store.dispatch(TestAction(second))

        assertThat(store.devToolsState.computedStates.size).isEqualTo(3)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(3)
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState(second))
        assertThat(store.devToolsState.currentAction).isEqualTo(TestAction(second))

        recomputeTestReducer.updated = true
        store.dispatch(DevToolsAction.createRecomputeAction())

        assertThat(store.devToolsState.computedStates.size).isEqualTo(3)
        assertThat(store.devToolsState.stagedActions.size).isEqualTo(3)
        assertThat(store.devToolsState.computedStates[1]).isEqualTo(TestState("updated $first"))
        assertThat(store.devToolsState.currentAppState).isEqualTo(TestState("updated $second"))
        assertThat(store.devToolsState.currentAction).isEqualTo(TestAction(second))
    }
}
