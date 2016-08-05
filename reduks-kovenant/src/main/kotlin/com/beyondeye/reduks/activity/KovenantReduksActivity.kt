package com.beyondeye.reduks.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ReduksModule
import com.beyondeye.reduksAndroid.activity.ReduksActivity
import nl.komponents.kovenant.android.startKovenant
import nl.komponents.kovenant.android.stopKovenant

/**
 * Created by daely on 6/13/2016.
 */
abstract class KovenantReduksActivity<S>: AppCompatActivity(), ReduksActivity<S> {
    override lateinit var reduks: Reduks<S>
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
    open fun initReduks() {
        reduks = ReduksModule<S>(
                ReduksModule.Def<S>(
                activityReduksContext(),
                KovenantStore.Factory<S>(),
                activityStartState(),
                activityStartAction(),
                getActivityStateReducer(),
                StoreSubscriberBuilder<S> {getActivityStoreSubscriber(it)})
        )
    }

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }

}