package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Store

/**
 * Created by daely on 8/4/2016.
 * ctx= ReduksContext associated to this Store
 */
abstract class MultiStore(@JvmField val ctx: ReduksContext) {
    /**
     *     map of all modules with  [ReduksContext] as index
     */
    abstract val storeMap:Map<String, Store<out Any>>
    @JvmField internal var dispatchWrappedAction: (Any) -> Any = { action ->
        when(action) {
            is ActionWithContext -> {
                val actionContext=action.context
                val selectedStore=storeMap[actionContext.toString()]
                if(selectedStore==null)
                    throw IllegalArgumentException("no registered module with context $actionContext")
                selectedStore.dispatch(action.action)
            }
            else -> throw IllegalArgumentException("Action missing context $action")
        }
    }
}

inline fun <reified SB : Any> MultiStore.subStore_(subctx: ReduksContext?):Store<SB>? =
        @Suppress("UNCHECKED_CAST")
        (storeMap[subctx?.toString() ?: ReduksContext.defaultModuleId<SB>()] as? Store<SB>)
