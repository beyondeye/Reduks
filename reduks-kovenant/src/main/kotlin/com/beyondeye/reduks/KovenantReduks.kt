package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.AsyncActionMiddleWare
import com.beyondeye.reduks.middlewares.applyMiddleware
import com.beyondeye.reduks.middlewares.applyStandardMiddlewares

/**
 * Redux based on [SimpleStore]
 * Created by daely on 6/8/2016.
 */
class KovenantReduks<State>(startState:State, startAction:Any,
                            reducer:Reducer<State>,
                            getSubscriber:(Store<State>) -> StoreSubscriber<State>) :Reduks<State>{
    override val store: Store<State>
    override val storeSubscriber: StoreSubscriber<State>
    override val storeSubscription: StoreSubscription
    init {
        store = KovenantStore(startState,reducer)
        store.applyStandardMiddlewares()
        store.applyMiddleware(AsyncActionMiddleWare())
        storeSubscriber=getSubscriber(store)
        storeSubscription = store.subscribe(storeSubscriber)
        store.dispatch(startAction)
    }
}
