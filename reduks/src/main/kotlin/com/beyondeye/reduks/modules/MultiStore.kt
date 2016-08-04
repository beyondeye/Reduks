package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Store

/**
 * Created by daely on 8/4/2016.
 */
abstract class MultiStore {
    /**
     * ReduksContext associated to this Store
     */
    abstract val ctx:ReduksContext
    /**
     *     map of all modules with  [ReduksContext] as index
     */
    abstract val storeMap:Map<ReduksContext, Store<out Any>>
    internal var dispatchWrappedAction: (Any) -> Any = { action ->
        when(action) {
            is ActionWithContext -> {
                val actionContext=action.context
                val selectedStore=storeMap[actionContext]
                if(selectedStore==null)
                    throw IllegalArgumentException("no registered module with context $actionContext")
                selectedStore.dispatch(action.action)
            }
            else -> throw IllegalArgumentException("Action missing context $action")
        }
    }
}