package com.beyondeye.reduks

/**
 * combine multiple store enhancers to a single one
 * Created by daely on 8/24/2016.
 */

private fun <S> StoreEnhancer<S>.toLambda(): (StoreCreator<S>)  -> StoreCreator<S> = { e -> this.enhance(e)}

//TODO refactor compose (used also for middleware) to single place
private  fun <T> compose(functions: List<(T) -> T>): (T) -> T {
    return { x -> functions.foldRight(x, { f, composed -> f(composed) }) }
}

fun <S> combineEnhancers(vararg enhancers: StoreEnhancer<S>)=
        StoreEnhancerFn<S> { storeCreator ->
            compose(enhancers.map { it.toLambda() })(storeCreator)
        }

