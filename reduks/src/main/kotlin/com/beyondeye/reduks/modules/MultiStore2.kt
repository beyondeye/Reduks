package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*

/**
 * Created by daely on 8/3/2016.
 */
open class MultiStore2<S1:Any,S2:Any>(
        val store1:Store<S1>, val ctx1:ReduksContext,
        val store2:Store<S2>, val ctx2:ReduksContext) :Store<MultiState2<S1,S2>>
{
    class Factory<S1:Any,S2:Any>(val storeFactory: StoreFactory<Any>,val ctx1:ReduksContext,val ctx2:ReduksContext): StoreFactory< MultiState2<S1,S2> > {
        override fun newStore(initialState: MultiState2<S1, S2>, reducer: Reducer<MultiState2<S1, S2>>): Store<MultiState2<S1, S2>> {
            if(reducer !is MultiReducer2<S1,S2>) throw IllegalArgumentException()
            return MultiStore2<S1,S2>(storeFactory.ofType<S1>().newStore(initialState.s1,reducer.r1),ctx1,storeFactory.ofType<S2>().newStore(initialState.s2,reducer.r2),ctx2)
        }

        override fun <S2> ofType(): StoreFactory<S2> =storeFactory.ofType<S2>()
        override val storeStandardMiddlewares: Array<out Middleware<MultiState2<S1, S2>>> =
                storeFactory.ofType<MultiState2<S1,S2>>().storeStandardMiddlewares

    }
    override val state: MultiState2<S1, S2> get()= MultiState2(store1.state,store2.state)
    internal var dispatchWrappedAction: (Any) -> Any = { action ->
        when(action) {
            is ActionWithContext -> {
                dispatchActionWithContext(action)
            }
            else -> throw IllegalArgumentException("Action missing context $action")
        }
    }
    override var dispatch=dispatchWrappedAction
    override fun subscribe(storeSubscriber: StoreSubscriber<MultiState2<S1, S2>>): StoreSubscription {
        val s1=store1.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        val s2=store2.subscribe(StoreSubscriber { storeSubscriber.onStateChange(state) })
        return MultiStoreSubscription(s1, s2)
    }
    /*
    val store1:Store<S1>
    val store2:Store<S2>
    */
    fun dispatchActionWithContext(a: ActionWithContext): Any = when (a.context) {
        ctx1 -> store1.dispatch(a.action)
        ctx2 -> store2.dispatch(a.action)
        else -> throw IllegalArgumentException("no registered module with id ${a.context.moduleId}")
    }
}