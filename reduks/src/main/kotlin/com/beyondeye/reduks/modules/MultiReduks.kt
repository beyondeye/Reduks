package com.beyondeye.reduks.modules

/**
 * same interface as regular reduks but allows to delegate actions to installed modules
 * Created by daely on 7/31/2016.
 */


/**
 * base class for all MultiReduksN generic classes (MultiReduks2, MultiReduks3, ....)
 */
abstract class MultiReduks {
    /**
     *     map of all modules with  [ReduksContext] as index
     */
    abstract val rmap:Map<ReduksContext,ReduksModule<out Any>>
    internal var dispatchWrappedAction: (Any) -> Any = { action ->
        when(action) {
            is ActionWithContext -> {
                dispatchActionWithContext(action)
            }
            else -> throw IllegalArgumentException("Action missing context $action")
        }
    }
    internal abstract fun dispatchActionWithContext(a: ActionWithContext): Any
    companion object {
        fun <S1:Any,S2:Any>buildFromModules(m1: IReduksModuleDef<S1>,
                                            m2: IReduksModuleDef<S2>)=MultiReduks2(m1, m2)
        fun <S1:Any,S2:Any,S3:Any>buildFromModules(m1: IReduksModuleDef<S1>,
                                                   m2: IReduksModuleDef<S2>,
                                                   m3: IReduksModuleDef<S3>)=MultiReduks3(m1, m2, m3)
    }
}


