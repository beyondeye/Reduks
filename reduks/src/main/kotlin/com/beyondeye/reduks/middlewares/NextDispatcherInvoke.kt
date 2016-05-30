package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.NextDispatcher

operator fun NextDispatcher.invoke(action: Any):Any {
    return dispatch(action)
}