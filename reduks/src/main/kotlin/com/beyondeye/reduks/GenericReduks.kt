package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.applyMiddleware
import com.beyondeye.reduks.modules.ReduksContext
import com.beyondeye.reduks.modules.ReduksModuleDef

/**
 * generic redux Redux
 * TODO substistute KovenantReduks, SimpleReduks, and RxReduks using only GenericReduks and make them deprecated
 * Created by daely on 6/8/2016.
 */
class GenericReduks<State>(def: ReduksModuleDef<State>,val context: ReduksContext?=null) :Reduks<State> {
    override val store: Store<State>
    override val storeSubscriber: StoreSubscriber<State>
    override val storeSubscription: StoreSubscription
    init {
        val storeFactory= def.storeFactory
        store = storeFactory.newStore(def.initialState, def.stateReducer)
        store.applyMiddleware(*storeFactory.storeStandardMiddlewares)
        storeSubscriber= def.getStoreSubscriber()(store)
        storeSubscription = store.subscribe(storeSubscriber)
        if(context==null) //if we have a context, it means this reduks instance is actually part of a multireduks
            store.dispatch(def.startAction)
    }

    fun  subscribe(storeSubscriber: StoreSubscriber<State>): StoreSubscription =store.subscribe(storeSubscriber)
    //TODO add special dispatch
    /*
    in action creators, several dispatch variants:
	variant 1: as now: implicit type argument: the state associated with the reduks object, instance id=""
	variant 2: type argument for dispatch the state type to which the action refer to
	variant 3: type argument for dispatch plus instance argument
     */
}
