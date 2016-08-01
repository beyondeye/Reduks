package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ActionContext
import com.beyondeye.reduks.modules.ActionWithContext


/**
 * A middleware for unwrapping an action that has some module context
 */

class WrapActionMiddleware<S>(val actionContext:ActionContext) : Middleware<S> {
    override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any):Any {
        if(action !is ActionWithContext) {
            return next(ActionWithContext(action,actionContext))
        }
        return  next(action)
    }

}
