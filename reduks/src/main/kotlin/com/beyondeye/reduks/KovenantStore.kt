package com.beyondeye.reduks

import nl.komponents.kovenant.Promise
import nl.komponents.kovenant.task
import nl.komponents.kovenant.then
import java.util.ArrayList

/**
 * Store that use kovenant promises for synchronizing action dispatches and notification to store subscribers
 * TODO add  option to make always sure that subscribers are called on the main android thread
 * this is the most common use case
 */
class KovenantStore<S>(initialState: S, val reducer: Reducer<S>) : Store<S> {
    private var _statePromise: Promise<S,Exception> = task {initialState}
    val statePromise: Promise<S,Exception> get() =_statePromise
    override val state: S
        get() = _statePromise.get() //wait completion of all current queued promises
    private val subscribers = ArrayList<StoreSubscriber<S>>()
    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any):Any {
            synchronized(this) { //make sure that writes to statePromise are synchronized
                _statePromise = _statePromise.then { startState ->
                    val newState=reducer.reduce(startState, action) //return newState
                    newState
                } then { newState->
                    for (i in subscribers.indices) {
                        subscribers[i].onStateChange(newState)
                    }
                    newState
                }
            }
            return action;
        }
    }

    /**
     * dispach an action to the store and return it (eventually after it is transformed by middlewares)
     * An action can be of Any type
     */
    override var dispatch: (action: Any) -> Any = { action ->
        mainDispatcher.dispatch(this, NullDispatcher(),action )
    }

    fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
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

