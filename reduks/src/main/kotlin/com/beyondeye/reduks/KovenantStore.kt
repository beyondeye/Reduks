package com.beyondeye.reduks

import nl.komponents.kovenant.*
import nl.komponents.kovenant.ui.promiseOnUi
import java.util.ArrayList

infix fun <V, R> Promise<V, Exception>.thenUi(bind: (V) -> R): Promise<R, Exception> {
    return this.then { promiseOnUi { bind(it) }.get() }
}
    /**
 * Store that use kovenant promises for synchronizing action dispatches and notification to store subscribers
 */
class KovenantStore<S>(initialState: S, val reducer: Reducer<S>, val observeOnUiThread:Boolean=true) : Store<S> {
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
                }.then { newState->
                    if(!observeOnUiThread) {
                        for (i in subscribers.indices) {
                            subscribers[i].onStateChange(newState)
                        }
                        newState
                    } else {
                        promiseOnUi {
                            for (i in subscribers.indices) {
                                subscribers[i].onStateChange(newState)
                            }
                            newState
                        }.get()
                    }
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

