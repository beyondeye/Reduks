package com.beyondeye.reduksAndroid.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beyondeye.reduks.*


/**
 * An activity base class for avoiding writing boilerplate code for initializing reduks and handling save and restoring reduks state
 * on onSaveInstanceState/onRestoreInstanceState activity life-cycle events
 * automatically handle save and restore of store state on activity recreation using a special custom action [ActionRestoreState]
 * IMPORTANT NOTE: this base activity uses the [SimpleStore] implementation or reduks [Store]. For similar base activities that
 *                 use different store implementation, looks for AsyncReduksActivity in reduks-async module (uses AsyncStore), or
 *                 RxReduksActivity in reduks-rx module
 *
 * Created by daely on 6/13/2016.
 */
abstract class SimpleReduksActivity<S>: ReduksActivity<S>, AppCompatActivity() {
    lateinit override var reduks: Reduks<S>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reduks=initReduks()
        reduks.store.errorLogFn= ReduksActivity.defaultReduksInternalLogger
    }

    override fun <T> storeCreator(): StoreCreator<T> = SimpleStore.Creator<T>()

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        ActionRestoreState.saveReduksState(this,reduks,outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        ActionRestoreState.restoreReduksState(this,reduks,savedInstanceState)
    }

}