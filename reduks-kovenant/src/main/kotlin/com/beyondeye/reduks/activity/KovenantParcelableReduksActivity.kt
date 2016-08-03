package com.beyondeye.reduks.activity

import android.os.Bundle
import android.os.Parcelable
import com.beyondeye.reduks.KovenantStore
import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.StoreSubscriberBuilder
import com.beyondeye.reduks.combineReducers
import com.beyondeye.reduks.modules.ReduksModule

/**
 * same as KovenantReduksActivity but for Parcelable state S, automatically handle save and
 * restore of store state on activity recreation using a special custom action ActionRestoreState
 * Created by daely on 6/27/2016.
 */
abstract class KovenantParcelableReduksActivity<S: Parcelable> : KovenantReduksActivity<S>() {
    class ActionRestoreState<S>(val restoredState:S)

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putParcelable(REDUKS_STATE, reduks.store.state)
        super.onSaveInstanceState(outState)
    }
    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        val restoredState:S?=savedInstanceState?.getParcelable(REDUKS_STATE)
        if(restoredState!=null) {
            reduks.dispatch(ActionRestoreState(restoredState))
        }
    }

    override fun initReduks() {
        reduks = ReduksModule<S>(
                ReduksModule.Def<S>(
                        KovenantStore.Factory(),
                        activityStartState(),
                        activityStartAction(),
                        combineReducers(getActivityStateReducer(), restoreStateReducer),
                        StoreSubscriberBuilder { getActivityStoreSubscriber(it) }
                )
        )
    }

    @Suppress("UNCHECKED_CAST")
    val restoreStateReducer = Reducer<S> { state, action ->
        when (action) {
            is ActionRestoreState<*> -> action.restoredState as S
            else -> state
        }
    }

    companion object {
        private val REDUKS_STATE: String?="REDUKS_STATE"
    }
}