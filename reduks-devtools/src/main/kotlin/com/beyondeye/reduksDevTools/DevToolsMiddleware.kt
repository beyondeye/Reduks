package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.Middleware
import com.beyondeye.reduks.NextDispatcher
import com.beyondeye.reduks.Store

class DevToolsMiddleware<S>(private val store: Store<S>, private val appMiddleware: Middleware<S>) : Middleware<DevToolsState<S>> {

    override fun dispatch(devToolsStore: Store<DevToolsState<S>>, next: NextDispatcher, action: Any): Any {
        // Actions are wrapped by the dispatcher as a com.beyondeye.reduksDevTools.DevToolsAction. However, the middleware passed
        // into the constructor act on original app actions. Therefore, we must lift the app action
        // out of the om.beyondeye.reduksDevTools.DevToolsAction container.
        var actionToDispatch = action

        if (action is DevToolsAction && action.appAction != null) {
            actionToDispatch = action.appAction
        }

        val dispatcher = NextDispatcher { action ->
            // Since next can be called within any Middleware, we need to wrap the actions
            // as DevToolsActions, in the same way as we wrap the initial dispatch call.
            if (action is DevToolsAction) {
                next.dispatch(action)
            } else {
                next.dispatch(DevToolsAction.createPerformAction(action))
            }
            action
        }
        appMiddleware.dispatch(store, dispatcher, actionToDispatch)
        return action
    }
}
