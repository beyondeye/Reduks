package com.beyondeye.reduks

import org.assertj.core.api.Assertions
import org.junit.Test

class Action1
class Action2
class Action3

data class TestState(val actionList:String="")

val r1 = ReducerFn<TestState> { state, action ->
    when (action) {
        is Action1 -> state.copy(actionList = state.actionList+"r1a1 ")
        is Action2 -> state.copy(actionList = state.actionList+"r1a2 ")
        else -> state
    }
}
val r2 = ReducerFn<TestState> { state, action ->
    when (action) {
        is Action1 -> state.copy(actionList = state.actionList+"r2a1 ")
        is Action2 -> state.copy(actionList = state.actionList+"r2a2 ")
        else -> state
    }
}
val r3 = ReducerFn<TestState> { state, action ->
    when (action) {
        is Action1 -> state.copy(actionList = state.actionList+"r3a1 ")
        is Action2 -> state.copy(actionList = state.actionList+"r3a2 ")
        else -> state
    }
}

/**
 * Created by daely on 1/12/2017.
 */
class CombineReducersTests {
    @Test
    fun test_CombineTwoReducerWithPlusOperator() {
        val store = SimpleStore(TestState(), r1+r2)

        store.dispatch(Action1())
        Assertions.assertThat(store.state.actionList).isEqualTo("r1a1 r2a1 ")
        store.dispatch(Action2())
        Assertions.assertThat(store.state.actionList).isEqualTo("r1a1 r2a1 r1a2 r2a2 ")
    }
    @Test
    fun test_CombineThreeReducerWithPlusOperator() {
        val store = SimpleStore(TestState(), r1+r2+r3)

        store.dispatch(Action1())
        Assertions.assertThat(store.state.actionList).isEqualTo("r1a1 r2a1 r3a1 ")
        store.dispatch(Action2())
        Assertions.assertThat(store.state.actionList).isEqualTo("r1a1 r2a1 r3a1 r1a2 r2a2 r3a2 ")
    }
    @Test
    fun test_CombineThreeReducerWithCombinedWith() {
        val store = SimpleStore(TestState(), r1.combinedWith(r2,r3))

        store.dispatch(Action1())
        Assertions.assertThat(store.state.actionList).isEqualTo("r1a1 r2a1 r3a1 ")
        store.dispatch(Action2())
        Assertions.assertThat(store.state.actionList).isEqualTo("r1a1 r2a1 r3a1 r1a2 r2a2 r3a2 ")
    }
}