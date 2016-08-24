package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ActionWithContext
import com.beyondeye.reduks.modules.ReduksContext


/**
 * A middleware for unwrapping an action that has some reduks context
 */

class WrapActionMiddleware<S>(val context: ReduksContext) : Middleware<S> {
    override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any):Any {
        if(action !is ActionWithContext) {
            return next(ActionWithContext(action,context))
        }
        return  next(action)
    }

}
