package com.beyondeye.reduks.middlewares

import com.beyondeye.reduks.*


//this is helper function basically perform currying on the provided middleware function
private  fun<P1, P2, P3, R> Function3<P1, P2, P3, R>.curried(): (P1) -> (P2) -> (P3) -> R {
    return { p1: P1 -> { p2: P2 -> { p3: P3 -> this(p1, p2, p3) } } }
}
private  fun <T> compose(functions: List<(T) -> T>): (T) -> T {
    return { x -> functions.foldRight(x, { f, composed -> f(composed) }) }
}

/**
* next is the next dispatcher to call after this middleware, to process the action dispatched to the store
*/
private  fun <S> Middleware<S>.toLambda(): (store_: Store<S>, nextDispatcher:(Any)->Any, action:Any )  -> Any
{
    return { store,next,action -> this.dispatch(store, {it ->next(it)}, action)}
}



/**
 * a middleware function is a method with signature
 *
 * fun myMiddleware<S,A>(store:Store<S,A>, nextDispatcher:(Action)->Action, action:Action) : Action
 * An action can be of Any type
 *
 * by calling next, we can post new actions to be processed by all chained middleware that follows
 * the next function is basically the next middleware function with parameters 'store' and 'next' already bound
 */
fun <S> Store<S>.applyMiddleware(
        vararg middlewares: Middleware<S>
):  Store<S> {
    dispatch = compose(middlewares.map { it.toLambda().curried()(this) })(dispatch)
    return  this
}


fun <S> Middleware<S>.toEnhancer(): StoreEnhancer<S> {
    return StoreEnhancerFn { srcStoreCreator->
        object:StoreCreator<S> {
            override fun create(reducer: Reducer<S>, initialState: S)=
               srcStoreCreator.create(reducer,initialState).applyMiddleware(this@toEnhancer)
            override fun <S_> ofType(): StoreCreator<S_> =srcStoreCreator.ofType()
        }
    }
}

fun <S> Array<Middleware<S>>.toEnhancer(): StoreEnhancer<S> {
    return StoreEnhancerFn { srcStoreCreator->
        object:StoreCreator<S> {
            override fun create(reducer: Reducer<S>, initialState: S): Store<S> =
                    srcStoreCreator.create(reducer,initialState).applyMiddleware(*this@toEnhancer)
            override fun <S_> ofType(): StoreCreator<S_> =srcStoreCreator.ofType()
        }
    }
}
