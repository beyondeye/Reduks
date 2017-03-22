package com.beyondeye.reduks.bus

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.ReducerFn

fun <S : StateWithBusData> busDataReducer(): Reducer<S> {
    return ReducerFn { s, a ->
        when (a) {
            is ActionSendBusData -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithUpdatedBusData(a.key, a.newData) as S
            }
            is ActionClearBusData -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithRemovedBusData(a.key) as S
            }
            else -> s
        }
    }
}