package com.beyondeye.reduksAndroid.activity

import androidx.lifecycle.ViewModel
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import com.beyondeye.reduks.Action
import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.Reduks
import java.io.Serializable

internal class ReduksStateViewModel(var savedState: SavedToViewModel?=null) : ViewModel()

/**
 * Action that is automatical dispatched when activity is resumed, with the reduks state value
 */
class ActionRestoreState<S>(val restoredState:S): Action {
    companion object {
        @Suppress("UNCHECKED_CAST")
        internal fun<S> getRestoreStateReducer() = ReducerFn<S> { state, action ->
            when (action) {
                is ActionRestoreState<*> -> action.restoredState as S
                else -> state
            }
        }

        fun <S> saveReduksState(activity: FragmentActivity, reduks: Reduks<S>, outState: Bundle?) {
            val curReduksState = reduks.store.state
            when (curReduksState) {
                is Parcelable -> outState?.putParcelable(REDUKS_STATE, curReduksState)
                is Serializable -> outState?.putSerializable(REDUKS_STATE, curReduksState)
                is SavedToViewModel -> {
                    val reduksStateViewModel = ViewModelProvider(activity).get(ReduksStateViewModel::class.java)
                    reduksStateViewModel.savedState = curReduksState
                }
            }
        }

        fun <S> restoreReduksState(activity: FragmentActivity, reduks: Reduks<S>, savedInstanceState: Bundle?) {
            @Suppress("UNCHECKED_CAST")
            val restoredState:S? = when(reduks.store.state) {
                is Parcelable ->savedInstanceState?.getParcelable<Parcelable>(REDUKS_STATE) as? S
                is Serializable -> savedInstanceState?.getSerializable(REDUKS_STATE) as? S
                is SavedToViewModel -> ViewModelProvider(activity).get(ReduksStateViewModel::class.java).savedState as? S

                else -> null
            }
            //the restored state
            restoredState?.let { reduks.store.dispatch(ActionRestoreState(it)) }
        }
        private val REDUKS_STATE: String?="REDUKS_STATE"

    }
}