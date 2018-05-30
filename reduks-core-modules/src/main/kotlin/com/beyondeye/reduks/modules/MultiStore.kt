package com.beyondeye.reduks.modules

import com.beyondeye.reduks.SagaAction
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
    @JvmField
    internal var dispatchWrappedAction: (Any) -> Any = { action ->

        when (action) {
            is ActionWithContext -> {
                dispatchToSubstore(action)
            }
            is MultiActionWithContext -> {
                action.actionList.forEach { if (it != null) dispatchToSubstore(it) }
            }
        /**
         *nothing to do: see documentation of [SagaAction]
         */
            is SagaAction -> {
            }
            else -> throw IllegalArgumentException("Action missing context $action")
        }
    }

    private fun dispatchToSubstore(action: ActionWithContext): Any {
        val actionContext = action.context
        val selectedStore: Store<out Any>? = subStore_(actionContext)
        if (selectedStore == null)
            throw IllegalArgumentException("no registered module with context $actionContext")
        return selectedStore.dispatch(action.action)
    }
}
inline fun <reified SB : Any> MultiStore.subStore_(subctx: ReduksContext?):Store<SB>? {
    val res: Store<out Any>? =
            if (subctx == null) {
                storeMap[ReduksContext.defaultModuleId<SB>()]
            } else if (subctx.hasEmptyPath()) {
                storeMap[subctx.moduleId]
            } else {
                lateinit var lst: MultiStore
                subctx.modulePath!!.forEach {
                    val nxtlst = storeMap[it]
                    if (nxtlst !is MultiStore) return null
                    lst = nxtlst
                }
                lst.storeMap[subctx.moduleId]
            }
    @Suppress("UNCHECKED_CAST")
    return res as? Store<SB>
}
