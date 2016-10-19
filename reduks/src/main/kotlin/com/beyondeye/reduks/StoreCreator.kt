package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.applyMiddleware

/**
 * Factory for some specific Store type
 * Created by daely on 7/31/2016.
 */
interface StoreCreator<S> {
    /**
     * create a new store associated to this specific factory type
     */
    fun create(reducer: Reducer<S>, initialState: S):Store<S>
}
fun<S> StoreCreator<S>.enhancedWith(vararg enhancers: StoreEnhancer<S>):StoreCreator<S> {
    return combineEnhancers(*enhancers).enhance(this)
}

fun <S> StoreCreator<S>.withMiddlewares(vararg middlewares: Middleware<S>):StoreCreator<S> = StoreCreatorWithMiddlewares(this,*middlewares)

class StoreCreatorWithMiddlewares<S>(val creator:StoreCreator<S>,vararg middlewares_: Middleware<S>):StoreCreator<S> {
    val middlewares=middlewares_
    override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
        val res=creator.create(reducer,initialState)
        return res.applyMiddleware(*middlewares)
    }
}

/**
 * create an enhanced store
 * extension method, so we save on method count
 */
fun<S> StoreCreator<S>.create(
        reducer: Reducer<S>,
        initialState: S,
        enhancer: StoreEnhancer<S>): Store<S> {
    return enhancer.enhance(this).create(reducer, initialState)
}