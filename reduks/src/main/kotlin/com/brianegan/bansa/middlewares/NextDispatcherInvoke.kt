package com.brianegan.bansa.middlewares

import com.brianegan.bansa.NextDispatcher

operator fun NextDispatcher.invoke(action: Any):Any {
    return dispatch(action)
}