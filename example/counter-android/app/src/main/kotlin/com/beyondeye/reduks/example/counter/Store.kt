package com.beyondeye.reduks.example.counter

import com.beyondeye.reduks.SimpleStore

/**
 * Created by kittinunf on 9/1/16.
 */

val counterStore = SimpleStore(CounterState(), counterReducer())
