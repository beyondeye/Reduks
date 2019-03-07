package com.beyondeye.reduksAndroid.fragment

import com.beyondeye.reduks.*

internal fun StateWithFragmentStatusData.newStateWithUpdatedFragmentActiveStatus(fragmentTag: String, newFragmentStatus: FragmentActiveStatus): StateWithFragmentStatusData {
    return copyWithNewFragmentStatus(fragmentStatus.withUpdatedFragmentActiveStatus(fragmentTag,newFragmentStatus))
}

internal fun StateWithFragmentStatusData.newStateWithUpdatedFragmentCurAtPos(newFragmentTag: String, positionTag: String): StateWithFragmentStatusData {
    if(positionTag.isEmpty()) return this //if invalid position tag, ignore the update
    return copyWithNewFragmentStatus(fragmentStatus.withUpdatedFragmentCurAtPos(newFragmentTag,positionTag))
}


fun <S : StateWithFragmentStatusData> fragmentStatusReducer(): Reducer<S> {
    return ReducerFn { s, a ->
        when (a) {
            is ActionSetFragmentActiveStatus -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithUpdatedFragmentActiveStatus(a.fragmentTag,a.newActiveStatus) as S
            }
            is ActionSetFragmentCurAtPos -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithUpdatedFragmentCurAtPos(a.newFragmentTag,a.positionTag) as S
            }
            else -> s
        }
    }
}

