package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/3/2016.
 */
class MultiStore3<S1 : Any, S2 : Any, S3 : Any>(
        ctx1: ReduksContext, val store1: Store<S1>,
        ctx2: ReduksContext, val store2: Store<S2>,
        ctx3: ReduksContext, val store3: Store<S3>) : Store<MultiState3<S1, S2, S3>>, MultiStore() {
    class Factory<S1 : Any, S2 : Any, S3 : Any>(val storeFactory: StoreFactory<Any>,
                                                val ctx1: ReduksContext,
                                                val ctx2: ReduksContext,
                                                val ctx3: ReduksContext) : StoreFactory<MultiState3<S1, S2, S3>> {
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

    override val ctx = ReduksModule.multiContext(ctx1, ctx2, ctx3)

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