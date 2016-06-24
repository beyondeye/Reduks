package com.beyondeye.reduks

import android.os.Bundle

/**
 * Created by daely on 6/13/2016.
 */
abstract class SimpleReduksActivity<S>: android.support.v7.app.AppCompatActivity(), ReduksActivity<S> {
    lateinit override var reduks: Reduks<S>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reduks = SimpleReduks<S>(activityStartState(), activityStartAction(), getActivityStateReducer(), getActivityStoreSubscriber())
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    //override for making this function visible to inheritors (by default in kotlin methods cannot be overriden
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }

}