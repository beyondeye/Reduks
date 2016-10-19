package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * use @JvmField for avoiding generation of useless getter methods
 * Created by daely on 8/3/2016.
 */
class MultiStore4<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(
        ctx1: ReduksContext, @JvmField val store1: Store<S1>,
        ctx2: ReduksContext, @JvmField val store2: Store<S2>,
        ctx3: ReduksContext, @JvmField val store3: Store<S3>,
        ctx4: ReduksContext, @JvmField val store4: Store<S4>) : Store<MultiState4<S1, S2, S3, S4>>, MultiStore(ReduksModule.multiContext(ctx1, ctx2, ctx3, ctx4)) {
    override fun replaceReducer(reducer: Reducer<MultiState4<S1, S2, S3, S4>>) {
        throw UnsupportedOperationException("MultiStore does not support replacing reducer")
    }

    class Factory<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(@JvmField val ctx1: ReduksContext,
                                                          @JvmField val creator1:StoreCreator<S1>,
                                                          @JvmField val ctx2: ReduksContext,
                                                          @JvmField val creator2:StoreCreator<S2>,
                                                          @JvmField val ctx3: ReduksContext,
                                                          @JvmField val creator3:StoreCreator<S3>,
                                                          @JvmField val ctx4: ReduksContext,
                                                          @JvmField val creator4:StoreCreator<S4>
                                                          ) : StoreCreator<MultiState4<S1, S2, S3, S4>> {
        override fun create(reducer: Reducer<MultiState4<S1, S2, S3, S4>>,
                            initialState: MultiState4<S1, S2, S3, S4>): Store<MultiState4<S1, S2, S3, S4>> {
            if (reducer !is MultiReducer4<S1, S2, S3, S4>) throw IllegalArgumentException()
            return MultiStore4<S1, S2, S3, S4>(
                    ctx1, creator1.create(reducer.r1, initialState.s1),
                    ctx2, creator2.create(reducer.r2, initialState.s2),
                    ctx3, creator3.create(reducer.r3, initialState.s3),
                    ctx4, creator4.create(reducer.r4, initialState.s4))
        }
    }

    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2,
            ctx3 to store3,
            ctx4 to store4)
    override val state: MultiState4<S1, S2, S3, S4> get() = MultiState4(ctx, store1.state, store2.state, store3.state, store4.state)
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