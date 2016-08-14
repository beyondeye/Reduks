package com.beyondeye.reduksAndroid.activity

import android.os.Bundle
import android.os.Parcelable
import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.Reduks

class ActionRestoreState<S>(val restoredState:S) {
    companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun<S> getRestoreStateReducer() = Reducer<S> { state, action ->
            when (action) {
                is ActionRestoreState<*> -> action.restoredState as S
                else -> state
            }
        }
        fun<S> saveReduksState(reduks: Reduks<S>, outState: Bundle?) {
            val curReduksState=reduks.store.state
            if(curReduksState is Parcelable) {
                outState?.putParcelable(REDUKS_STATE,curReduksState)
            }
        }
        fun<S> restoreReduksState(reduks: Reduks<S>, savedInstanceState: Bundle?) {
            if(reduks.store.state is Parcelable) {
                val restoredState:S?=savedInstanceState?.getParcelable<Parcelable>(REDUKS_STATE) as S?
                if(restoredState!=null) {
                    reduks.dispatch(ActionRestoreState(restoredState))
                }
            }
        }
        private val REDUKS_STATE: String?="REDUKS_STATE"

    }
}