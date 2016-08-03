package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

class MultiReduks3<S1:Any,S2:Any,S3:Any>(ctx1: ReduksContext, def1: IReduksModuleDef<S1>,
                                         ctx2: ReduksContext, def2: IReduksModuleDef<S2>,
                                         ctx3: ReduksContext, def3: IReduksModuleDef<S3>) : MultiReduks(), Reduks<MultiState3<S1, S2, S3>> {
    val r1= ReduksModule<S1>(def1, ctx1)
    val r2= ReduksModule<S2>(def2, ctx2)
    val r3= ReduksModule<S3>(def3, ctx3)
    override val rmap= mapOf(ctx1 to r1,ctx2 to r2,ctx3 to r3)
    override fun dispatchActionWithContext(a: ActionWithContext): Any = when (a.context) {
        r1.context -> r1.dispatch(a.action)
        r2.context -> r2.dispatch(a.action)
        r3.context -> r3.dispatch(a.action)
        else -> throw IllegalArgumentException("no registered module with id ${a.context.moduleId}")
    }
    override val store= object: Store<MultiState3<S1, S2, S3>> {
        override val state: MultiState3<S1, S2, S3> get()= MultiState3(r1.store.state, r2.store.state, r3.store.state)
        override var dispatch=dispatchWrappedAction
        override fun subscribe(storeSubscriber: StoreSubscriber<MultiState3<S1, S2, S3>>): StoreSubscription {
            val s1=r1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            val s2=r2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            val s3=r3.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
            return MultiStoreSubscription(s1, s2, s3)
        }
    }
    fun subscribe(storeSubscriber: StoreSubscriber<MultiState3<S1, S2, S3>>): StoreSubscription =store.subscribe(storeSubscriber)
    /* empty subscriber: if you want to add a subscriber on global state changes, call [subscribe] function above */
    override val storeSubscriber = StoreSubscriber<MultiState3<S1, S2, S3>> {}
    /* empty subscription: if you want to add a  susbscriber on global state changes, call [subscribe] function above */
    override val storeSubscription= StoreSubscription {}
    init {
        r1.dispatch(def1.startAction)
        r2.dispatch(def2.startAction)
        r3.dispatch(def3.startAction)
    }
}