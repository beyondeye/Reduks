package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/3/2016.
 */
class MultiReduksDef {
    companion object {
        fun <S1 : Any, S2 : Any> create(storeFactory_: StoreFactory<MultiState2<S1, S2>>,
                                        m1: IReduksModuleDef<S1>, ctx1: ReduksContext,
                                        m2: IReduksModuleDef<S2>, ctx2: ReduksContext): IReduksModuleDef<MultiState2<S1, S2>> =
                object : IReduksModuleDef<MultiState2<S1, S2>>,IModuleDef2 {
                    override val storeFactory= MultiStore2.Factory<S1, S2>(storeFactory_.ofType(),ctx1,ctx2)
                    override val initialState = MultiState2(m1.initialState, m2.initialState)
                    override val startAction = MultiActionWithContext(ActionWithContext(m1.startAction, ctx1), ActionWithContext(m2.startAction, ctx2))
                    override val stateReducer: Reducer<MultiState2<S1, S2>> = object : MultiReducer2<S1,S2> {
                        override val r1: Reducer<S1>
                            get() = m1.stateReducer
                        override val r2: Reducer<S2>
                            get() = m2.stateReducer

                        override fun reduce(s: MultiState2<S1, S2>, a: Any): MultiState2<S1, S2> {
                            val actionList: List<Any> = MultiActionWithContext.toActionList(a)
                            var newS = s
                            actionList.forEach {
                                if (a is ActionWithContext) {
                                    newS = when (a.context) {
                                        ctx1 -> newS.copy(s1 = m1.stateReducer.reduce(newS.s1, a.action))
                                        ctx2 -> newS.copy(s2 = m2.stateReducer.reduce(newS.s2, a.action))
                                        else -> newS
                                    }
                                }
                            }
                            return newS
                        }

                    }
                    override val subscriberBuilder: StoreSubscriberBuilder<MultiState2<S1, S2>> = StoreSubscriberBuilder { store ->
                        if(store !is MultiStore2<*, *>) throw IllegalArgumentException("error")
                        val selector = SelectorBuilder<MultiState2<S1, S2>>()
                        val s1sel = selector.withSingleField { s1 }
                        val s2sel = selector.withSingleField { s2 }
                        @Suppress("UNCHECKED_CAST")
                        val sub1 = m1.subscriberBuilder.build(store.store1 as Store<S1>)
                        @Suppress("UNCHECKED_CAST")
                        val sub2 = m2.subscriberBuilder.build(store.store2 as Store<S2>)
                        StoreSubscriber { newS ->
                            s1sel.onChangeIn(newS) {
                                sub1.onStateChange(newS.s1) }
                            s2sel.onChangeIn(newS) {
                                sub2.onStateChange(newS.s2) }
                        }
                    }

                }



    }
}

interface IModuleDef2 {

}
