package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/3/2016.
 * use @JvmField for avoiding generation of useless getter methods
 */
class MultiStore2<S1:Any,S2:Any>(
        ctx1:ReduksContext,
        @JvmField val store1:Store<S1>,
        ctx2:ReduksContext,
        @JvmField val store2:Store<S2>) :Store<MultiState2<S1,S2>>,MultiStore(ReduksModule.multiContext(ctx1,ctx2))
{
    override fun replaceReducer(reducer: Reducer<MultiState2<S1, S2>>) {
        throw UnsupportedOperationException("MultiStore does not support replacing reducer")
    }

    class Factory<S1:Any,S2:Any>(@JvmField val storeFactory: StoreFactory<Any>,
                                 @JvmField val ctx1:ReduksContext,
                                 @JvmField val ctx2:ReduksContext): StoreFactory< MultiState2<S1,S2> > {
        override fun newStore(initialState: MultiState2<S1, S2>,
                              reducer: Reducer<MultiState2<S1, S2>>): Store<MultiState2<S1, S2>> {
            if(reducer !is MultiReducer2<S1, S2>) throw IllegalArgumentException()
            return MultiStore2<S1,S2>(
                    ctx1,storeFactory.ofType<S1>().newStore(initialState.s1,reducer.r1),
                    ctx2,storeFactory.ofType<S2>().newStore(initialState.s2,reducer.r2))
        }
        override fun <S_> ofType(): StoreFactory<S_> =storeFactory.ofType<S_>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState2<S1, S2>>> =
                storeFactory.ofType<MultiState2<S1,S2>>().storeStandardMiddlewares

    }
    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2)
    override val state: MultiState2<S1, S2> get()= MultiState2(ctx,store1.state,store2.state)
    override var dispatch=dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState2<S1, S2>>): StoreSubscription {
        val s1=store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s2=store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        return MultiStoreSubscription(s1, s2)
    }
}