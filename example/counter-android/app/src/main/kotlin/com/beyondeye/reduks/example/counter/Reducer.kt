package com.beyondeye.reduks.example.counter

import com.beyondeye.reduks.Reducer

/**
 * Created by kittinunf on 9/1/16.
 */

fun counterReducer(): Reducer<CounterState> = ReducerFn { state, action ->
    when (action) {
        is CounterAction.Init -> CounterState()
        is CounterAction.Increment -> {
            val value = state.counter
            state.copy(counter = value + action.count)
        }
        is CounterAction.Decrement -> {
            val value = state.counter
            state.copy(counter = value - action.count)
        }
        else -> state
    }
}