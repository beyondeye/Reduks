package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * use @JvmField for avoiding generation of useless getter methods
 * Created by daely on 8/3/2016.
 */
class MultiStore3<S1 : Any, S2 : Any, S3 : Any>(
        ctx1: ReduksContext, @JvmField val store1: Store<S1>,
        ctx2: ReduksContext, @JvmField val store2: Store<S2>,
        ctx3: ReduksContext, @JvmField val store3: Store<S3>) : Store<MultiState3<S1, S2, S3>>, MultiStore(ReduksModule.multiContext(ctx1, ctx2, ctx3)) {
    class Factory<S1 : Any, S2 : Any, S3 : Any>(@JvmField val storeFactory: StoreFactory<Any>,
                                                @JvmField val ctx1: ReduksContext,
                                                @JvmField val ctx2: ReduksContext,
                                                @JvmField val ctx3: ReduksContext) : StoreFactory<MultiState3<S1, S2, S3>> {
        override fun newStore(initialState: MultiState3<S1, S2, S3>,
                              reducer: Reducer<MultiState3<S1, S2, S3>>): Store<MultiState3<S1, S2, S3>> {
            if (reducer !is MultiReducer3<S1, S2, S3>) throw IllegalArgumentException()
            return MultiStore3<S1, S2, S3>(
                    ctx1, storeFactory.ofType<S1>().newStore(initialState.s1, reducer.r1),
                    ctx2, storeFactory.ofType<S2>().newStore(initialState.s2, reducer.r2),
                    ctx3, storeFactory.ofType<S3>().newStore(initialState.s3, reducer.r3))
        }

        override fun <S_> ofType(): StoreFactory<S_> = storeFactory.ofType<S_>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState3<S1, S2, S3>>> =
                storeFactory.ofType<MultiState3<S1, S2, S3>>().storeStandardMiddlewares

    }

    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2,
            ctx3 to store3)
    override val state: MultiState3<S1, S2, S3> get() = MultiState3(ctx, store1.state, store2.state, store3.state)
    override var dispatch = dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState3<S1, S2, S3>>): StoreSubscription {
        val s1 = store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s2 = store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s3 = store3.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        return MultiStoreSubscription(s1, s2, s3)
    }
}