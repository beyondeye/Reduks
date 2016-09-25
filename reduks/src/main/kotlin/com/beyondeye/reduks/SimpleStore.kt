package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.ThunkMiddleware

class SimpleStore<S>(initialState: S, private var reducer: IReducer<S>) : Store<S> {
    override fun replaceReducer(reducer: IReducer<S>) {
        this.reducer=reducer
    }
    class Creator<S>: StoreCreator<S> {
        override fun create(reducer: IReducer<S>, initialState: S): Store<S> = SimpleStore<S>(initialState,reducer)
        override val storeStandardMiddlewares:Array<IMiddleware<S>> = arrayOf(ThunkMiddleware<S>())
        override fun <S_> ofType(): StoreCreator<S_> {
            return Creator<S_>()
        }
    }
    override var state: S = initialState
    private val subscribers = mutableListOf<IStoreSubscriber<S>>()
    private val mainDispatcher = object : IMiddleware<S> {
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

    override fun subscribe(storeSubscriber: IStoreSubscriber<S>): IStoreSubscription {
        this.subscribers.add(storeSubscriber)
        return object : IStoreSubscription {
            override fun unsubscribe() {
                subscribers.remove(storeSubscriber)
            }
        }
    }
}

