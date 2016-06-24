package com.beyondeye.reduks.rx

import com.beyondeye.reduks.Reducer
import com.beyondeye.reduks.Reduks
import com.beyondeye.reduks.middlewares.applyStandardMiddlewares

/**
 * Reduks based on [RxStore]
 * Created by daely on 6/8/2016.
 */
class RxReduks<State>(startState:State, startAction:Any,
                      reducer: Reducer<State>,
                      getSubscriber:(RxStore<State>) -> RxStoreSubscriber<State>,
                      allRxSubscriptions: rx.subscriptions.CompositeSubscription?) : Reduks<State> {
    override val store: RxStore<State>
    override val storeSubscriber: RxStoreSubscriber<State>
    override val storeSubscription: RxStoreSubscription<State>
    init {
        store = RxStore(startState,reducer,allRxSubscriptions)
        store.applyStandardMiddlewares()
        storeSubscriber=getSubscriber(store)
        storeSubscription = store.subscribe(storeSubscriber)
        store.dispatch(startAction)
    }
}
