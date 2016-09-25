package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ActionWithContext
import com.beyondeye.reduks.modules.ReduksContext


/**
 * A middleware for unwrapping an action that has some reduks context
 */
class WrapActionMiddleware<S>(val context: ReduksContext) : IMiddleware<S> {
    override fun dispatch(store: Store<S>, nextDispatcher: (Any)->Any, action: Any):Any {
        if(action !is ActionWithContext) {
            return nextDispatcher(ActionWithContext(action,context))
        }
        return  nextDispatcher(action)
    }

}
