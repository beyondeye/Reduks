package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.applyStandardMiddlewares

/**
 * Redux based on [SimpleStore]
 * Created by daely on 6/8/2016.
 */
class Reduks<State>(startState:State,startAction:Any,
                    reducer:Reducer<State>,
                    getSubscriber:(Store<State>) -> StoreSubscriber<State>) {
    val store: Store<State>
    val storeSubscriber: StoreSubscriber<State>
    val storeSubscription: StoreSubscription
    init {
        store = SimpleStore(startState,reducer)
        store.applyStandardMiddlewares()
        storeSubscriber=getSubscriber(store)
        storeSubscription = store.subscribe(storeSubscriber)
        store.dispatch(startAction)
    }

    /**
     * call dispatch on the store object
     */
    fun dispatch(action:Any) = store.dispatch(action)
}
