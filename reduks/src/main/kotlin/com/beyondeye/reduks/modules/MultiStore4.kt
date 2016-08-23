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

    class Factory<S1 : Any, S2 : Any, S3 : Any, S4 : Any>(@JvmField val storeCreator: StoreCreator<Any>,
                                                          @JvmField val ctx1: ReduksContext,
                                                          @JvmField val ctx2: ReduksContext,
                                                          @JvmField val ctx3: ReduksContext,
                                                          @JvmField val ctx4: ReduksContext) : StoreCreator<MultiState4<S1, S2, S3, S4>> {
        override fun newStore(initialState: MultiState4<S1, S2, S3, S4>,
                              reducer: Reducer<MultiState4<S1, S2, S3, S4>>): Store<MultiState4<S1, S2, S3, S4>> {
            if (reducer !is MultiReducer4<S1, S2, S3, S4>) throw IllegalArgumentException()
            return MultiStore4<S1, S2, S3, S4>(
                    ctx1, storeCreator.ofType<S1>().newStore(initialState.s1, reducer.r1),
                    ctx2, storeCreator.ofType<S2>().newStore(initialState.s2, reducer.r2),
                    ctx3, storeCreator.ofType<S3>().newStore(initialState.s3, reducer.r3),
                    ctx4, storeCreator.ofType<S4>().newStore(initialState.s4, reducer.r4))
        }

        override fun <S_> ofType(): StoreCreator<S_> = storeCreator.ofType<S_>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState4<S1, S2, S3, S4>>> =
                storeCreator.ofType<MultiState4<S1, S2, S3, S4>>().storeStandardMiddlewares

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
        val s1 = store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s2 = store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s3 = store3.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s4 = store4.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        return MultiStoreSubscription(s1, s2, s3, s4)
    }
}