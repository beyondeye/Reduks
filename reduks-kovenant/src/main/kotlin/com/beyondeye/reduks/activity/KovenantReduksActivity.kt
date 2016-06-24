package com.beyondeye.reduks.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.*
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

/**
 * Created by daely on 6/13/2016.
 */
abstract class KovenantReduksActivity<S>: AppCompatActivity() {
    lateinit var reduks: Reduks<S>
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Configure Kovenant with standard dispatchers for android (See http://kovenant.komponents.nl/android/config/)
        startKovenant() //(before  initReduks()!!)
        initReduks()
    }

    override fun onDestroy() {
        // Dispose of the Kovenant thread pools
        // for quicker shutdown you could use
        // force=true, which ignores all current
        // scheduled tasks
        // see  (See http://kovenant.komponents.nl/android/config/)
        stopKovenant()
        super.onDestroy()
    }
    fun initReduks() {
        reduks = KovenantReduks(activityStartState(), activityStartAction(), getActivityStateReducer(), getActivityStoreSubscriber())
    }

    /**
     * return the initial state of the activity
     */
    abstract fun activityStartState():S

    /**
     * return the initial action to dispatch to the RxStore in onCreate
     */
    abstract fun activityStartAction():Any

    /**
     * return the activity state reducer
     */
    abstract fun getActivityStateReducer(): Reducer<S>

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }

    /**
     * return the activity main store subscriber
     */
    abstract fun getActivityStoreSubscriber():(Store<S>) -> StoreSubscriber<S>

}