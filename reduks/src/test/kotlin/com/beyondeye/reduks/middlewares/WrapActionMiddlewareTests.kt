package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.SimpleStore
import com.beyondeye.reduks.modules.ActionContext
import com.beyondeye.reduks.modules.ActionWithContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

/**
 * tests for WrapActionMiddleware and UnwrapActionMiddleware
 * Created by daely on 8/1/2016.
 */
class WrappedActionMiddlewareTests {
    data class TestState(val lastWrappedActionType: String = "",val lastActionType:String="")
    data class TestAction(val type: String)
    val reducer = Reducer<TestState> { state, action ->
        when (action) {
            is ActionWithContext -> {
                val unwrappedAction=action.action as TestAction
                state.copy(lastWrappedActionType = unwrappedAction.type)
            }
            is TestAction -> state.copy(lastActionType = action.type)
            else -> state
        }
    }
    val actionContext=ActionContext("moduleid")
    @Test
    fun test_action_get_wrapped() {
        //----GIVEN
        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(WrapActionMiddleware<TestState>(actionContext))
        //----WHEN
        store.dispatch(TestAction(type = "1"))
        //----THEN
        assertThat(store.state.lastActionType).isEqualTo("")
        assertThat(store.state.lastWrappedActionType).isEqualTo("1")
    }
    @Test
    fun test_action_dont_get_wrapped_twice() {
        //-----GIVEN
        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(WrapActionMiddleware<TestState>(actionContext))
        //------WHEN
        store.dispatch(ActionWithContext(TestAction(type = "1"),actionContext))
        //------THEN
        assertThat(store.state.lastActionType).isEqualTo("")
        assertThat(store.state.lastWrappedActionType).isEqualTo("1")
    }
    @Test
    fun test_wrap_and_unwrap_middlewares_cancel_each_other_Effect() {
        //----GIVEN
        val store = SimpleStore(TestState(), reducer)
        store.applyMiddleware(UnwrapActionMiddleware<TestState>())
        store.applyMiddleware(WrapActionMiddleware<TestState>(actionContext)) //LAST APPLIED MIDDLEWARE IS APPLIED FIRST
        //----WHEN
        store.dispatch(TestAction(type = "1"))
        //----THEN
        assertThat(store.state.lastActionType).isEqualTo("1")
        assertThat(store.state.lastWrappedActionType).isEqualTo("")
    }
}