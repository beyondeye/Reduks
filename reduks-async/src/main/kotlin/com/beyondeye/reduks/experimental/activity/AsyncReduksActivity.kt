package com.beyondeye.reduks.experimental.activity

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.beyondeye.reduks.*
import com.beyondeye.reduks.bus.BusStore
import com.beyondeye.reduks.experimental.AsyncStore
import com.beyondeye.reduks.experimental.middlewares.saga.SagaMiddleWare
import com.beyondeye.reduks.middlewares.applyMiddleware
import com.beyondeye.reduksAndroid.activity.ActionRestoreState
import com.beyondeye.reduksAndroid.activity.ReduksActivity
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI

/**
 * An activity base class for avoiding writing boilerplate code for initializing reduks and handling save and restoring reduks state
 * on onSaveInstanceState/onRestoreInstanceState activity life-cycle events
 * automatically handle save and restore of store state on activity recreation using a special custom action [ActionRestoreState]
 * Created by daely on 6/13/2016.
 */
abstract class AsyncReduksActivity<S:Any>(
        /**
         * if true, then create activate sagaMiddleware,  and automatically stop it on activity destroy
         */
        val withSagaMiddleWare:Boolean=false
): ReduksActivity<S>, AppCompatActivity() {
    lateinit override var reduks: Reduks<S>
    /**
     * store the reference to sagaMiddleware somewhere so that we can automatically stop it when activity is stopped
     * and also we can use it to create child sagas
     */
    var sagaMiddleware: SagaMiddleWare<S>?=null
        private set
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reduks=initReduks()
        if(withSagaMiddleWare) {
            sagaMiddleware = SagaMiddleWare(reduks.store)
            reduks.store.applyMiddleware(sagaMiddleware!!)
        }
    }

    override fun <T> storeCreator(): StoreCreator<T> = AsyncStore.Creator<T>(reduceContext = CommonPool,subscribeContext = UI)

    //override for making this function visible to inheritors
    override fun onStop() {
        super.onStop()
    }
    //override for making this function visible to inheritors
    override fun onStart() {
        super.onStart()
    }
    override fun onDestroy() {
        var store=reduks.store
        if(store is BusStore)
            store = store.wrappedStore
        sagaMiddleware?.stopAll()
        (store as? AsyncStore)?.stopActors()
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