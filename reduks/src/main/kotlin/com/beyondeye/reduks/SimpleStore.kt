package com.beyondeye.reduks

import java.util.ArrayList

class SimpleStore<S>(initialState: S, val reducer: Reducer<S>) : Store<S> {
    class Factory<S>: StoreFactory<S> {
        override fun newStore(initialState: S, reducer: Reducer<S>): Store<S> = SimpleStore<S>(initialState,reducer)
    }
    override var state: S = initialState
    private val subscribers = ArrayList<StoreSubscriber<S>>()
    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any):Any {
            synchronized (this) {
                state = reducer.reduce(store.state, action)
            }
            for (i in subscribers.indices) {
                subscribers[i].onStateChange(state)
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
        mainDispatcher.dispatch(this, NullDispatcher(),action )
    }

    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        this.subscribers.add(storeSubscriber)
        return object : StoreSubscription {
            override fun unsubscribe() {
                subscribers.remove(storeSubscriber)
            }
        }
    }
    class NullDispatcher : NextDispatcher {
        override fun dispatch(action: Any): Any = action
    }
}

