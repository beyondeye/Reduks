package com.beyondeye.reduks.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.beyondeye.reduks.*
import com.beyondeye.reduksAndroid.activity.ActionRestoreState
import com.beyondeye.reduksAndroid.activity.ReduksActivity
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

/**
 * An activity base class for avoiding writing boilerplate code for initializing reduks and handling save and restoring reduks state
 * on onSaveInstanceState/onRestoreInstanceState activity life-cycle events
 * automatically handle save and restore of store state on activity recreation using a special custom action [ActionRestoreState]
 * Created by daely on 6/13/2016.
 */
abstract class KovenantReduksActivity<S>: ReduksActivity<S>, AppCompatActivity() {
    lateinit override var reduks: Reduks<S>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Kovenant with standard dispatchers for android (See http://kovenant.komponents.nl/android/config/)
        //startKovenant() //(before  initReduks()!!)
        reduks=initReduks()
        reduks.store.errorLogFn= ReduksActivity.defaultReduksInternalLogger
    }

    override fun <T> storeCreator(): StoreCreator<T> = KovenantStore.Creator<T>()

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }
    override fun onDestroy() {
        // Dispose of the Kovenant thread pools
        // for quicker shutdown you could use
        // force=true, which ignores all current
        // scheduled tasks
        // see  (See http://kovenant.komponents.nl/android/config/)
        //stopKovenant()
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