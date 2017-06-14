package com.beyondeye.reduks.experimental.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.*
import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduksAndroid.activity.ActionRestoreState
import com.beyondeye.reduksAndroid.activity.ReduksActivity
import kotlinx.coroutines.experimental.android.UI

/**
 * An activity base class for avoiding writing boilerplate code for initializing reduks and handling save and restoring reduks state
 * on onSaveInstanceState/onRestoreInstanceState activity life-cycle events
 * automatically handle save and restore of store state on activity recreation using a special custom action [ActionRestoreState]
 * Created by daely on 6/13/2016.
 */
abstract class AsyncReduksActivity<S>: ReduksActivity<S>, AppCompatActivity() {
    lateinit override var reduks: Reduks<S>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reduks=initReduks()
    }

    override fun <T> storeCreator(): StoreCreator<T> = AsyncStore.Creator<T>(subscribeContext = UI)

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }
    override fun onDestroy() {
        (reduks.store as AsyncStore).stopActors()
        super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle?) {
        ActionRestoreState.saveReduksState(reduks, outState)
        super.onSaveInstanceState(outState)
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        ActionRestoreState.restoreReduksState(reduks, savedInstanceState)
    }

}