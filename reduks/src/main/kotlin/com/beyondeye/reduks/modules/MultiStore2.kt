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

    class Factory<S1:Any,S2:Any>(@JvmField val storeCreator: StoreCreator<Any>,
                                 @JvmField val ctx1:ReduksContext,
                                 @JvmField val ctx2:ReduksContext): StoreCreator< MultiState2<S1,S2> > {
        override fun create(reducer: Reducer<MultiState2<S1, S2>>,
                            initialState: MultiState2<S1, S2>): Store<MultiState2<S1, S2>> {
            if(reducer !is MultiReducer2<S1, S2>) throw IllegalArgumentException()
            return MultiStore2<S1,S2>(
                    ctx1,storeCreator.ofType<S1>().create(reducer.r1, initialState.s1),
                    ctx2,storeCreator.ofType<S2>().create(reducer.r2, initialState.s2))
        }
        override fun <S_> ofType(): StoreCreator<S_> =storeCreator.ofType<S_>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState2<S1, S2>>> =
                storeCreator.ofType<MultiState2<S1,S2>>().storeStandardMiddlewares

    }
    override val storeMap = mapOf(
            ctx1 to store1,
            ctx2 to store2)
    override val state: MultiState2<S1, S2> get()= MultiState2(ctx,store1.state,store2.state)
    override var dispatch=dispatchWrappedAction
    //call back the multi subscriber each time any component state change
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState2<S1, S2>>): StoreSubscription {
        val s1=store1.subscribe(StoreSubscriberFn { storeSubscriber.onStateChange() })
        val s2=store2.subscribe(StoreSubscriberFn { storeSubscriber.onStateChange() })
        return MultiStoreSubscription(s1, s2)
    }
}