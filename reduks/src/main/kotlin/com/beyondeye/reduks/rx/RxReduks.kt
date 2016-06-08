package com.beyondeye.reduks.rx

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.middlewares.applyStandardMiddlewares

/**
 * Reduks based on [RxStore]
 * Created by daely on 6/8/2016.
 */
class RxReduks<State>(startState:State, startAction:Any,
                      reducer: Reducer<State>,
                      getSubscriber:(RxStore<State>) -> RxStoreSubscriber<State>,
                      allRxSubscriptions: rx.subscriptions.CompositeSubscription?) {
    val store: RxStore<State>
    val storeSubscriber: RxStoreSubscriber<State>
    val storeSubscription: RxStoreSubscription<State>
    init {
        store = RxStore(startState,reducer,allRxSubscriptions)
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
