package com.beyondeye.reduks

/**
 * Factory for some specific Store type
 * Created by daely on 7/31/2016.
 */
interface StoreCreator<S> {
    /**
     * create a new store associated to this specific factory type
     */
    fun create(reducer: IReducer<S>, initialState: S):Store<S>

    /**
     * get list of standard middlewares available for the type of Store associated with this factory
     */
    val storeStandardMiddlewares: Array<out IMiddleware<S>>

    /**
     * return new factory with same parameter but for new state type S2
     */
    fun <S_> ofType(): StoreCreator<S_>
}

/**
 * create an enhanced store
 * extension method, so we save on method count
 */
fun<S> StoreCreator<S>.create(
        reducer: IReducer<S>,
        initialState: S,
        enhancer: IStoreEnhancer<S>): Store<S> {
    return enhancer.enhance(this).create(reducer, initialState)
}