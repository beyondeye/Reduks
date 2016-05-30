package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.Reducer

class DevToolsTestReducer : Reducer<DevToolsReducerTest.TestState> {
    override fun reduce(state: DevToolsReducerTest.TestState, action: Any): DevToolsReducerTest.TestState {
        when (action) {
            is DevToolsReducerTest.TestAction -> {
                if (updated) {
                    return state.copy("updated ${action.message}")
                } else {
                    return state.copy(action.message)
                }
            }
            else -> return state
        }
    }

    var updated = false;
}
