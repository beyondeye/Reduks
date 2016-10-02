/**
 * Created by kittinunf on 9/2/16.
 */

package com.beyondeye.reduks.example.counter

import com.beyondeye.reduks.SimpleStore
import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.subscribe
import java.util.*

//STATE
data class CounterState(val counter: Int = 0)

//ACTION
sealed class CounterAction {
    object Init : CounterAction()
    object Increment : CounterAction()
    object Decrement : CounterAction()
}

//REDUCER
fun counterReducer(): Reducer<CounterState> = ReducerFn { state, action ->
    when (action) {
        is CounterAction.Init -> CounterState()
        is CounterAction.Increment -> {
            val value = state.counter
            state.copy(counter = value + 1)
        }
        is CounterAction.Decrement -> {
            val value = state.counter
            state.copy(counter = value - 1)
        }
        else -> state
    }
}

//STORE
val counterStore = SimpleStore(CounterState(), counterReducer())

fun main(args: Array<String>) {

    counterStore.subscribe {
        println("Current Counter value is ${counterStore.state}")
    }

    val scanner = Scanner(System.`in`)
    println("Welcome to Reduks Commandline app, type s to show current value, i to increment, d to decrement and q to quit program")
    var command: String? = null
    while (command != "q") {
        command = scanner.nextLine()

        when (command) {
            "s", "S" -> {
                println("Current Counter value is ${counterStore.state}")
            }
            "i", "I" -> {
                println("Increment!")
                counterStore.dispatch(CounterAction.Increment)
            }
            "d", "D" -> {
                println("Decrement")
                counterStore.dispatch(CounterAction.Decrement)
            }
            else -> println("Not supported, type only s, i, d or q to quit program")
        }
    }

    println("Bye!")
}
