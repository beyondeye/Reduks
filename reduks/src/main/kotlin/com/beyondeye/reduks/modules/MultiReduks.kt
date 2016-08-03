package com.beyondeye.reduks.modules

/**
 * same interface as regular reduks but allows to delegate actions to installed modules
 * Created by daely on 7/31/2016.
 */
data class MultiState2<S1:Any,S2:Any>(val s1:S1,val s2:S2)
data class MultiState3<S1:Any,S2:Any,S3:Any>(val s1:S1,val s2:S2,val s3:S3)
data class MultiState4<S1:Any,S2:Any,S3:Any,S4:Any>(val s1:S1,val s2:S2,val s3:S3,val s4:S4)
data class MultiState5<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any>(val s1:S1,val s2:S2,val s3:S3,val s4:S4,val s5:S5)
data class MultiState6<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any,S6:Any>(val s1:S1,val s2:S2,val s3:S3,val s4:S4,val s5:S5,val s6:S6)


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
        fun <S1:Any,S2:Any>buildFromModules(m1:ReduksModule.Def<S1>,ctx1:ReduksContext,
                                            m2:ReduksModule.Def<S2>,ctx2:ReduksContext)=MultiReduks2(m1,ctx1,m2,ctx2)
    }
}


