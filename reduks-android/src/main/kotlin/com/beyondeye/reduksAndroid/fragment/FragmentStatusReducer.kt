package com.beyondeye.reduksAndroid.fragment

import com.beyondeye.reduks.*
import java.lang.ref.WeakReference

internal fun StateWithFragmentStatusData.newStateWithUpdatedFragmentStatus(fragmentTag: String, newFragmentStatus: FragmentStatus): StateWithFragmentStatusData {
    return copyWithNewFragmentStatus(fragmentStatus.plus(fragmentTag,newFragmentStatus))
}


fun <S : StateWithFragmentStatusData> fragmentStatusReducer(): Reducer<S> {
    return ReducerFn { s, a ->
        when (a) {
            is ActionSetFragmentStatus -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithUpdatedFragmentStatus(a.fragmentTag,a.newStatus) as S
            }
            else -> s
        }
    }
}

