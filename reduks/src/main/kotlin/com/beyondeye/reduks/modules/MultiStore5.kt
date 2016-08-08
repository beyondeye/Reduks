package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * use @JvmField for avoiding generation of useless getter methods
 * Created by daely on 8/3/2016.
 */
class MultiStore5<S1 : Any, S2 : Any, S3 : Any, S4 : Any, S5 : Any>(
        ctx1: ReduksContext, @JvmField val store1: Store<S1>,
        ctx2: ReduksContext, @JvmField val store2: Store<S2>,
        ctx3: ReduksContext, @JvmField val store3: Store<S3>,
        ctx4: ReduksContext, @JvmField val store4: Store<S4>,
        ctx5: ReduksContext, @JvmField val store5: Store<S5>) : Store<MultiState5<S1, S2, S3, S4, S5>>, MultiStore() {
    class Factory<S1 : Any, S2 : Any, S3 : Any, S4 : Any, S5 : Any>(@JvmField val storeFactory: StoreFactory<Any>,
                                                                    @JvmField val ctx1: ReduksContext,
                                                                    @JvmField val ctx2: ReduksContext,
                                                                    @JvmField val ctx3: ReduksContext,
                                                                    @JvmField val ctx4: ReduksContext,
                                                                    @JvmField val ctx5: ReduksContext) : StoreFactory<MultiState5<S1, S2, S3, S4, S5>> {
        override fun newStore(initialState: MultiState5<S1, S2, S3, S4, S5>,
                              reducer: Reducer<MultiState5<S1, S2, S3, S4, S5>>): Store<MultiState5<S1, S2, S3, S4, S5>> {
            if (reducer !is MultiReducer5<S1, S2, S3, S4, S5>) throw IllegalArgumentException()
            return MultiStore5<S1, S2, S3, S4, S5>(
                    ctx1, storeFactory.ofType<S1>().newStore(initialState.s1, reducer.r1),
                    ctx2, storeFactory.ofType<S2>().newStore(initialState.s2, reducer.r2),
                    ctx3, storeFactory.ofType<S3>().newStore(initialState.s3, reducer.r3),
                    ctx4, storeFactory.ofType<S4>().newStore(initialState.s4, reducer.r4),
                    ctx5, storeFactory.ofType<S5>().newStore(initialState.s5, reducer.r5))
        }

        override fun <S_> ofType(): StoreFactory<S_> = storeFactory.ofType<S_>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState5<S1, S2, S3, S4, S5>>> =
                storeFactory.ofType<MultiState5<S1, S2, S3, S4, S5>>().storeStandardMiddlewares

    }

    override val ctx = ReduksModule.multiContext(ctx1, ctx2, ctx3, ctx4, ctx5)

    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2,
            ctx3 to store3,
            ctx4 to store4,
            ctx5 to store5)
    override val state: MultiState5<S1, S2, S3, S4, S5> get() = MultiState5(ctx, store1.state, store2.state, store3.state, store4.state, store5.state)
    override var dispatch = dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState5<S1, S2, S3, S4, S5>>): StoreSubscription {
        val s1 = store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s2 = store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s3 = store3.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s4 = store4.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s5 = store5.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        return MultiStoreSubscription(s1, s2, s3, s4, s5)
    }
}