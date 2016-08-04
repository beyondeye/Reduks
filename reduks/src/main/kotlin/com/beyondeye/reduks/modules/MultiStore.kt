package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Store

/**
 * Created by daely on 8/4/2016.
 */
abstract class MultiStore {
    /**
     *     map of all modules with  [ReduksContext] as index
     */
    abstract val storeMap:Map<ReduksContext, Store<out Any>>
    internal var dispatchWrappedAction: (Any) -> Any = { action ->
        when(action) {
            is ActionWithContext -> {
                dispatchActionWithContext(action)
            }
            else -> throw IllegalArgumentException("Action missing context $action")
        }
    }
    abstract fun dispatchActionWithContext(a: ActionWithContext): Any
}