package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.applyMiddleware

/**
 * generic redux Redux
 * TODO substistute KovenantReduks, SimpleReduks, and RxReduks using only ReduksModule and make them deprecated
 * Created by daely on 6/8/2016.
 */
class ReduksModule<State>(def: ReduksModuleDef<State>, val context: ReduksContext?=null) : Reduks<State> {
    override val store: Store<State>
    override val storeSubscriber: StoreSubscriber<State>
    override val storeSubscription: StoreSubscription
    init {
        val storeFactory= def.storeFactory
        store = storeFactory.newStore(def.initialState, def.stateReducer)
        store.applyMiddleware(*storeFactory.storeStandardMiddlewares)
        storeSubscriber= def.subscriberBuilder.build(store)
        storeSubscription = store.subscribe(storeSubscriber)
        if(context==null) //if we have a context, it means this reduks instance is actually part of a multireduks
            store.dispatch(def.startAction)
    }
    /*
    constructor(startState:State, startAction:Any,
                reducer: Reducer<State>,
                getSubscriber:(Store<State>) -> StoreSubscriber<State>):this(object:ReduksModuleDef<State> {}) {

    }
    */
    fun  subscribe(storeSubscriber: StoreSubscriber<State>): StoreSubscription =store.subscribe(storeSubscriber)
}
