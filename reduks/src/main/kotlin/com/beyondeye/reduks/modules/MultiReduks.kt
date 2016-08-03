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
        fun <S1:Any,S2:Any>buildFromModules(ctx1: ReduksContext, m1: IReduksModuleDef<S1>,
                                            ctx2: ReduksContext, m2: IReduksModuleDef<S2>)=MultiReduks2(ctx1, m1, ctx2, m2)
        fun <S1:Any,S2:Any,S3:Any>buildFromModules(ctx1: ReduksContext, m1: IReduksModuleDef<S1>,
                                                   ctx2: ReduksContext, m2: IReduksModuleDef<S2>,
                                                   ctx3: ReduksContext, m3: IReduksModuleDef<S3>)=MultiReduks3(ctx1, m1, ctx2, m2, ctx3, m3)
        fun <S1:Any,S2:Any,S3:Any,S4:Any>buildFromModules(ctx1: ReduksContext, m1: IReduksModuleDef<S1>,
                                                   ctx2: ReduksContext, m2: IReduksModuleDef<S2>,
                                                   ctx3: ReduksContext, m3: IReduksModuleDef<S3>,
                                                   ctx4: ReduksContext, m4: IReduksModuleDef<S4>)=MultiReduks4(ctx1, m1, ctx2, m2, ctx3, m3, ctx4, m4)
        fun <S1:Any,S2:Any,S3:Any,S4:Any,S5:Any>buildFromModules(ctx1: ReduksContext, m1: IReduksModuleDef<S1>,
                                                          ctx2: ReduksContext, m2: IReduksModuleDef<S2>,
                                                          ctx3: ReduksContext, m3: IReduksModuleDef<S3>,
                                                          ctx4: ReduksContext, m4: IReduksModuleDef<S4>,
                                                          ctx5: ReduksContext, m5: IReduksModuleDef<S5>)=MultiReduks5(ctx1, m1, ctx2, m2, ctx3, m3, ctx4, m4, ctx5, m5)
        fun <S1:Any,S2:Any,S3:Any,S4:Any,S5:Any,S6:Any>buildFromModules(ctx1: ReduksContext, m1: IReduksModuleDef<S1>,
                                                                 ctx2: ReduksContext, m2: IReduksModuleDef<S2>,
                                                                 ctx3: ReduksContext, m3: IReduksModuleDef<S3>,
                                                                 ctx4: ReduksContext, m4: IReduksModuleDef<S4>,
                                                                 ctx5: ReduksContext, m5: IReduksModuleDef<S5>,
                                                                 ctx6: ReduksContext, m6: IReduksModuleDef<S6>)=MultiReduks6(ctx1, m1, ctx2, m2, ctx3, m3, ctx4, m4, ctx5, m5, ctx6, m6)
    }
}


