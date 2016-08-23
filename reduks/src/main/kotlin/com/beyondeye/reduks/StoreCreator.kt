package com.beyondeye.reduks

/**
 * Factory for some specific Store type
 * Created by daely on 7/31/2016.
 */
interface StoreCreator<S> {
    /**
     * create a new store associated to this specific factory type
     */
    fun newStore(initialState:S, reducer: Reducer<S>):Store<S>

    /**
     * get list of standard middlewares available for the type of Store associated with this factory
     */
    val storeStandardMiddlewares: Array<out Middleware<S>>

    /**
     * return new factory with same parameter but for new state type S2
     */
    fun<S_> ofType():StoreCreator<S_>
}