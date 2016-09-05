package com.beyondeye.reduks.example.counter

/**
 * Created by kittinunf on 9/1/16.
 */

sealed class CounterAction {
    object Init : CounterAction()
    class Increment(val count: Int) : CounterAction()
    class Decrement(val count: Int) : CounterAction()
}
