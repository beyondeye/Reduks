package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ActionWithContext


/**
 * A middleware for unwrapping an action that has some module context
 */

class UnwrapActionMiddleware<S> : Middleware<S> {
    override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any):Any {
        if(action is ActionWithContext) {
            return next(action.action)
        }
        return  next(action)
    }

}
