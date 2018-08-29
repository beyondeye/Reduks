package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * use @JvmField for avoiding generation of useless getter methods
 * Created by daely on 8/3/2016.
 */
class MultiStore4<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(
        ctx1: ReduksContext, @JvmField val store1: Store<S1>, @JvmField val subscription1:StoreSubscription?,
        ctx2: ReduksContext, @JvmField val store2: Store<S2>, @JvmField val subscription2:StoreSubscription?,
        ctx3: ReduksContext, @JvmField val store3: Store<S3>, @JvmField val subscription3:StoreSubscription?,
        ctx4: ReduksContext, @JvmField val store4: Store<S4>, @JvmField val subscription4:StoreSubscription?) : Store<MultiState4<S1, S2, S3, S4>>, MultiStore(ReduksModule.multiContext(ctx1, ctx2, ctx3, ctx4)) {
    override var errorLogFn: ((String) -> Unit)?
        get() = store1.errorLogFn
        set(value) {
            store1.errorLogFn=value
            store2.errorLogFn=value
            store3.errorLogFn=value
            store4.errorLogFn=value
        }

    override fun replaceReducer(reducer: Reducer<MultiState4<S1, S2, S3, S4>>) {
        throw UnsupportedOperationException("MultiStore does not support replacing reducer: replace the substate reducer instead")
    }

    internal class Factory<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(@JvmField val ctx1: ReduksContext,
                                                                   @JvmField val creator1:StoreCreator<S1>,
                                                                   @JvmField val sub1:StoreSubscriberBuilder<S1>?,
                                                                   @JvmField val ctx2: ReduksContext,
                                                                   @JvmField val creator2:StoreCreator<S2>,
                                                                   @JvmField val sub2:StoreSubscriberBuilder<S2>?,
                                                                   @JvmField val ctx3: ReduksContext,
                                                                   @JvmField val creator3:StoreCreator<S3>,
                                                                   @JvmField val sub3:StoreSubscriberBuilder<S3>?,
                                                                   @JvmField val ctx4: ReduksContext,
                                                                   @JvmField val creator4:StoreCreator<S4>,
                                                                   @JvmField val sub4:StoreSubscriberBuilder<S4>?
    ) : StoreCreator<MultiState4<S1, S2, S3, S4>> {
        override fun create(reducer: Reducer<MultiState4<S1, S2, S3, S4>>,
                            initialState: MultiState4<S1, S2, S3, S4>): Store<MultiState4<S1, S2, S3, S4>> {
            if (reducer !is MultiReducer4<S1, S2, S3, S4>) throw IllegalArgumentException()
            val store1 = creator1.create(reducer.r1, initialState.s1)
            val store2 = creator2.create(reducer.r2, initialState.s2)
            val store3 = creator3.create(reducer.r3, initialState.s3)
            val store4 = creator4.create(reducer.r4, initialState.s4)
            return MultiStore4<S1, S2, S3, S4>(
                    ctx1, store1,store1.subscribe(sub1),
                    ctx2, store2,store2.subscribe(sub2),
                    ctx3, store3,store3.subscribe(sub3),
                    ctx4, store4,store4.subscribe(sub4))

        }
    }

    override val storeMap = mapOf(
            ctx1.moduleId to store1,
            ctx2.moduleId to store2,
            ctx3.moduleId to store3,
            ctx4.moduleId to store4)
    override val state: MultiState4<S1, S2, S3, S4> get() = MultiState4(store1.state, store2.state, store3.state, store4.state)
    override var dispatch = dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState4<S1, S2, S3, S4>>): StoreSubscription {
        val s1 = store1.subscribe(StoreSubscriberFn { storeSubscriber.onStateChange() })
        val s2 = store2.subscribe(StoreSubscriberFn { storeSubscriber.onStateChange() })
        val s3 = store3.subscribe(StoreSubscriberFn { storeSubscriber.onStateChange() })
        val s4 = store4.subscribe(StoreSubscriberFn { storeSubscriber.onStateChange() })
        return MultiStoreSubscription(s1, s2, s3, s4)
    }
}