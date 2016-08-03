package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.applyMiddleware

/**
 * generic redux Redux
 * TODO substistute KovenantReduks, SimpleReduks, and RxReduks using only ReduksModule and make them deprecated
 * Created by daely on 6/8/2016.
 */
class ReduksModule<State>(moduleDef: IReduksModuleDef<State>, val context: ReduksContext?=null) : Reduks<State> {
    /**
     * all data needed for creating a ReduksModule
     */
    data class Def<State>(
            /**
             * factory method for store
             */
            override val  storeFactory:StoreFactory<State>,
            /**
             * return the initial state
             */
            override val initialState: State,
            /**
             * return the initial action to dispatch to the Store
             */
            override val startAction: Any,
            /**
             * return the state reducer
             */
            override val stateReducer: Reducer<State>,
            /**
             * return the main store subscriber
             * we pass as argument the store itself, so that we can create an object that implement the
             * [StoreSubscriber] interface that keep a reference to the store itself, in case the we need call dispatch
             * in the subscriber
             */
            override val subscriberBuilder: StoreSubscriberBuilder<State>)  : IReduksModuleDef<State>

    override val store: Store<State>
    override val storeSubscriber: StoreSubscriber<State>
    override val storeSubscription: StoreSubscription
    init {
        val storeFactory= moduleDef.storeFactory
        store=newStore(moduleDef, storeFactory)
        store.applyMiddleware(*storeFactory.storeStandardMiddlewares)
        storeSubscriber= moduleDef.subscriberBuilder.build(store)
        storeSubscription = store.subscribe(storeSubscriber)
        if(context==null) //if we have a context, it means this reduks instance is actually part of a multireduks
        {
            //split multiaction to list if required
            val actionList: List<Any> = MultiActionWithContext.toActionList(moduleDef.startAction)
            actionList.forEach { store.dispatch(it) }
        }
    }
    private fun newStore(moduleDef: IReduksModuleDef<State>, storeFactory: StoreFactory<State>):Store<State> {
        return storeFactory.newStore(moduleDef.initialState, moduleDef.stateReducer)
    }

    fun  subscribe(storeSubscriber: StoreSubscriber<State>): StoreSubscription =store.subscribe(storeSubscriber)
    companion object {

    }
}
