package com.beyondeye.reduks.example.counter.ui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.StoreSubscription
import com.beyondeye.reduks.example.counter.CounterAction
import com.beyondeye.reduks.example.counter.R
import com.beyondeye.reduks.example.counter.counterStore
import com.beyondeye.reduks.example.counter.util.observeStore
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var subscription: StoreSubscription

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscription = observeStore(counterStore) {
            counterTextView.text = it.counter.toString()
        }

        counterStore.dispatch(CounterAction.Init)

        incrementButton.setOnClickListener {
            counterStore.dispatch(createIncrementAction(1))
        }

        decrementButton.setOnClickListener {
            counterStore.dispatch(createDecrementAction(1))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription.unsubscribe()
    }

    fun createIncrementAction(value: Int): CounterAction = CounterAction.Increment(1)
    fun createDecrementAction(value: Int): CounterAction = CounterAction.Decrement(1)

}
