package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

class MultiReduks5<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any>(def1: ReduksModule.Def<S1>, ctx1: ReduksContext,
                                         def2: ReduksModule.Def<S2>, ctx2: ReduksContext,
                                         def3: ReduksModule.Def<S3>, ctx3: ReduksContext,
                                         def4: ReduksModule.Def<S4>, ctx4: ReduksContext,
                                         def5: ReduksModule.Def<S5>, ctx5: ReduksContext) : MultiReduks(), Reduks<MultiState5<S1, S2, S3, S4, S5>> {
    val r1= ReduksModule<S1>(def1, ctx1)
    val r2= ReduksModule<S2>(def2, ctx2)
    val r3= ReduksModule<S3>(def3, ctx3)
    val r4= ReduksModule<S4>(def4, ctx4)
    val r5= ReduksModule<S5>(def5, ctx5)
    override val rmap= mapOf(ctx1 to r1,ctx2 to r2,ctx3 to r3,ctx4 to r4,ctx5 to r5)
    override fun dispatchActionWithContext(a: ActionWithContext): Any = when (a.context) {
        r1.context -> r1.dispatch(a.action)
        r2.context -> r2.dispatch(a.action)
        r3.context -> r3.dispatch(a.action)
        r4.context -> r4.dispatch(a.action)
        r5.context -> r5.dispatch(a.action)
        else -> throw IllegalArgumentException("no registered module with id ${a.context.moduleId}")
    }
    override val store= object: Store<MultiState5<S1, S2, S3, S4, S5>> {
        override val state: MultiState5<S1, S2, S3, S4, S5> get()= MultiState5(r1.store.state, r2.store.state, r3.store.state, r4.store.state, r5.store.state)
        override var dispatch=dispatchWrappedAction
        override fun subscribe(storeSubscriber: StoreSubscriber<MultiState5<S1, S2, S3, S4, S5>>): StoreSubscription {
            val s1=r1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            val s2=r2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            val s3=r3.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            val s4=r4.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            val s5=r5.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            return MultiStoreSubscription(s1, s2, s3, s4, s5)
        }
    }
    fun subscribe(storeSubscriber: StoreSubscriber<MultiState5<S1, S2, S3, S4, S5>>): StoreSubscription =store.subscribe(storeSubscriber)
    /* empty subscriber: if you want to add a subscriber on global state changes, call [subscribe] function above */
    override val storeSubscriber = StoreSubscriber<MultiState5<S1, S2, S3, S4, S5>> {}
    /* empty subscription: if you want to add a  susbscriber on global state changes, call [subscribe] function above */
    override val storeSubscription= StoreSubscription {}
    init {
        r1.dispatch(def1.startAction)
        r2.dispatch(def2.startAction)
        r3.dispatch(def3.startAction)
        r4.dispatch(def4.startAction)
        r5.dispatch(def5.startAction)
    }
}