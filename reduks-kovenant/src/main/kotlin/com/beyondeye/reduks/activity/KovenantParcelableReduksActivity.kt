package com.beyondeye.reduks.activity

import android.os.Bundle
import android.os.Parcelable
import com.beyondeye.reduks.KovenantReduks
import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.combineReducers

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
    override  fun initReduks() {
        reduks = KovenantReduks(activityStartState(), activityStartAction(), combineReducers(getActivityStateReducer(),restoreStateReducer), getActivityStoreSubscriber())
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