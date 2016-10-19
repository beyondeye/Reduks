package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware

class SimpleStore<S>(initialState: S, private var reducer: Reducer<S>) : Store<S> {
    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer=reducer
    }
    class Creator<S>(val withStandardMiddlewares:Boolean=true): StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
          val res=SimpleStore<S>(initialState,reducer)
            return if(!withStandardMiddlewares)
                res
            else
                res.applyMiddleware(ThunkMiddleware())
        }
        override fun <S_> ofType(): StoreCreator<S_> {
            return Creator<S_>()
        }
    }
    override var state: S = initialState
    private val subscribers = mutableListOf<StoreSubscriber<S>>()
    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any):Any {
            try {
                synchronized(this) {
                    state = reducer.reduce(store.state, action)
                }
            } finally {
                subscribers.forEach { it.onStateChange() }
            }
            return action
        }
    }

    init {
        this.state = initialState
    }


    /**
     * dispach an action to the store and return it (eventually after it is transformed by middlewares)
     * An action can be of Any type
     */
    override var dispatch: (action: Any) -> Any = { action ->
        mainDispatcher.dispatch(this,
                {it->it}, //null dispatcher that ends the chain
                action )
    }

    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        this.subscribers.add(storeSubscriber)
        return object : StoreSubscription {
            override fun unsubscribe() {
                subscribers.remove(storeSubscriber)
            }
        }
    }
}

