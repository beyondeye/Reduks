package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/3/2016.
 */
class MultiStore4<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(
        ctx1: ReduksContext, val store1: Store<S1>,
        ctx2: ReduksContext, val store2: Store<S2>,
        ctx3: ReduksContext, val store3: Store<S3>,
        ctx4: ReduksContext, val store4: Store<S4>) : Store<MultiState4<S1, S2, S3, S4>>, MultiStore() {
    class Factory<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(val storeFactory: StoreFactory<Any>,
                                                                    val ctx1: ReduksContext,
                                                                    val ctx2: ReduksContext,
                                                                    val ctx3: ReduksContext,
                                                                    val ctx4: ReduksContext) : StoreFactory<MultiState4<S1, S2, S3, S4>> {
        override fun newStore(initialState: MultiState4<S1, S2, S3, S4>,
                              reducer: Reducer<MultiState4<S1, S2, S3, S4>>): Store<MultiState4<S1, S2, S3, S4>> {
            if (reducer !is MultiReducer4<S1, S2, S3, S4>) throw IllegalArgumentException()
            return MultiStore4<S1, S2, S3, S4>(
                    ctx1, storeFactory.ofType<S1>().newStore(initialState.s1, reducer.r1),
                    ctx2, storeFactory.ofType<S2>().newStore(initialState.s2, reducer.r2),
                    ctx3, storeFactory.ofType<S3>().newStore(initialState.s3, reducer.r3),
                    ctx4, storeFactory.ofType<S4>().newStore(initialState.s4, reducer.r4))
        }

        override fun <S_> ofType(): StoreFactory<S_> = storeFactory.ofType<S_>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState4<S1, S2, S3, S4>>> =
                storeFactory.ofType<MultiState4<S1, S2, S3, S4>>().storeStandardMiddlewares

    }

    override val ctx = MultiReduksDef.multiContext(ctx1, ctx2, ctx3, ctx4)

    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2,
            ctx3 to store3,
            ctx4 to store4)
    override val state: MultiState4<S1, S2, S3, S4> get() = MultiState4(ctx, store1.state, store2.state, store3.state, store4.state)
    override var dispatch = dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState4<S1, S2, S3, S4>>): StoreSubscription {
        val s1 = store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s2 = store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s3 = store3.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s4 = store4.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        return MultiStoreSubscription(s1, s2, s3, s4)
    }
}