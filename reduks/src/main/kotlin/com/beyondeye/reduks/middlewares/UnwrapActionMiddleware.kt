package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.ActionWithContext


/**
 * A middleware for unwrapping an action that has some reudks context
 */

class UnwrapActionMiddleware<S> : Middleware<S> {
    override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
        if(action is ActionWithContext) {
            return nextDispatcher(action.action)
        }
        return  nextDispatcher(action)
    }

}
