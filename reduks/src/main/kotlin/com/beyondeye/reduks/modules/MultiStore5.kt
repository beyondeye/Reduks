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
        ctx5: ReduksContext, @JvmField val store5: Store<S5>) : Store<MultiState5<S1, S2, S3, S4, S5>>, MultiStore( ReduksModule.multiContext(ctx1, ctx2, ctx3, ctx4, ctx5)) {
    override fun replaceReducer(reducer: IReducer<MultiState5<S1, S2, S3, S4, S5>>) {
        throw UnsupportedOperationException("MultiStore does not support replacing reducer")
    }

    class Factory<S1 : Any, S2 : Any, S3 : Any, S4 : Any, S5 : Any>(@JvmField val storeCreator: StoreCreator<Any>,
                                                                    @JvmField val ctx1: ReduksContext,
                                                                    @JvmField val ctx2: ReduksContext,
                                                                    @JvmField val ctx3: ReduksContext,
                                                                    @JvmField val ctx4: ReduksContext,
                                                                    @JvmField val ctx5: ReduksContext) : StoreCreator<MultiState5<S1, S2, S3, S4, S5>> {
        override fun create(reducer: IReducer<MultiState5<S1, S2, S3, S4, S5>>,
                            initialState: MultiState5<S1, S2, S3, S4, S5>): Store<MultiState5<S1, S2, S3, S4, S5>> {
            if (reducer !is MultiReducer5<S1, S2, S3, S4, S5>) throw IllegalArgumentException()
            return MultiStore5<S1, S2, S3, S4, S5>(
                    ctx1, storeCreator.ofType<S1>().create(reducer.r1, initialState.s1),
                    ctx2, storeCreator.ofType<S2>().create(reducer.r2, initialState.s2),
                    ctx3, storeCreator.ofType<S3>().create(reducer.r3, initialState.s3),
                    ctx4, storeCreator.ofType<S4>().create(reducer.r4, initialState.s4),
                    ctx5, storeCreator.ofType<S5>().create(reducer.r5, initialState.s5))
        }

        override fun <S_> ofType(): StoreCreator<S_> = storeCreator.ofType<S_>()
        override val storeStandardMiddlewares: Array<out IMiddleware<MultiState5<S1, S2, S3, S4, S5>>> =
                storeCreator.ofType<MultiState5<S1, S2, S3, S4, S5>>().storeStandardMiddlewares

    }

    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2,
            ctx3 to store3,
            ctx4 to store4,
            ctx5 to store5)
    override val state: MultiState5<S1, S2, S3, S4, S5> get() = MultiState5(ctx, store1.state, store2.state, store3.state, store4.state, store5.state)
    override var dispatch = dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: IStoreSubscriber<MultiState5<S1, S2, S3, S4, S5>>): IStoreSubscription {
        val s1 = store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange() })
        val s2 = store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange() })
        val s3 = store3.subscribe(StoreSubscriber { storeSubscriber.onStateChange() })
        val s4 = store4.subscribe(StoreSubscriber { storeSubscriber.onStateChange() })
        val s5 = store5.subscribe(StoreSubscriber { storeSubscriber.onStateChange() })
        return MultiStoreSubscription(s1, s2, s3, s4, s5)
    }
}