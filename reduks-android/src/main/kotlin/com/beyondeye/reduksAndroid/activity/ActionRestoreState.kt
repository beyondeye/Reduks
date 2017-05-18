package com.beyondeye.reduksAndroid.activity

import android.os.Bundle
import android.os.Parcelable
import com.beyondeye.reduks.Action
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.Reduks
import java.io.Serializable

class ActionRestoreState<S>(val restoredState:S): Action {
    companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun<S> getRestoreStateReducer() = ReducerFn<S> { state, action ->
            when (action) {
                is ActionRestoreState<*> -> action.restoredState as S
                else -> state
            }
        }
        fun<S> saveReduksState(reduks: Reduks<S>, outState: Bundle?) {
            val curReduksState=reduks.store.state
            when(curReduksState) {
                is Parcelable -> outState?.putParcelable(REDUKS_STATE,curReduksState)
                is Serializable-> outState?.putSerializable(REDUKS_STATE,curReduksState)
            }
        }
        fun<S> restoreReduksState(reduks: Reduks<S>, savedInstanceState: Bundle?) {
            val restoredState:S? = when(reduks.store.state) {
                is Parcelable ->savedInstanceState?.getParcelable<Parcelable>(REDUKS_STATE) as? S
                is Serializable -> savedInstanceState?.getSerializable(REDUKS_STATE) as? S
                else -> null
            }
            restoredState?.let { reduks.store.dispatch(ActionRestoreState(it)) }
        }
        private val REDUKS_STATE: String?="REDUKS_STATE"

    }
}