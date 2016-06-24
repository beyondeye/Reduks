package com.beyondeye.reduks

import nl.komponents.kovenant.*
import nl.komponents.kovenant.android.androidUiDispatcher
import nl.komponents.kovenant.ui.promiseOnUi
import java.util.ArrayList

infix fun <V, R> Promise<V, Exception>.thenUi(bind: (V) -> R): Promise<R, Exception> {
    return this.then { promiseOnUi { bind(it) }.get() }
}

/**
 * Store that use kovenant promises for synchronizing action dispatches and notification to store subscribers
 */
class KovenantStore<S>(initialState: S, val reducer: Reducer<S>, val observeOnUiThread: Boolean = true) : Store<S> {
    val observeContext =
            if (!observeOnUiThread)
            {
                Kovenant.context
            } else {
                Kovenant.createContext {
                    workerContext {
                        dispatcher = androidUiDispatcher()
                        errorHandler = Kovenant.context.workerContext.errorHandler
                    }
                    callbackContext {
                        dispatcher = Kovenant.context.callbackContext.dispatcher
                        errorHandler = Kovenant.context.callbackContext.errorHandler
                    }
                }
            }
    val defaultContext = Kovenant.context
    private var _statePromise: Promise<S, Exception> = task { initialState }
    val statePromise: Promise<S, Exception> get() = _statePromise
    override val state: S
        get() = _statePromise.get() //wait completion of all current queued promises
    private val subscribers = ArrayList<StoreSubscriber<S>>()
    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, next: NextDispatcher, action: Any): Any {
            val deferredNextState = deferred<S, Exception>()
            var curStatePromise: Promise<S, Exception>? = null
            synchronized(this) {
                curStatePromise = _statePromise
                _statePromise = deferredNextState.promise
            }

            curStatePromise?.then(defaultContext) { startState ->
                reducer.reduce(startState, action) //return newState
            }?.then(observeContext) { newState ->
                for (i in subscribers.indices) {
                    subscribers[i].onStateChange(newState)
                }
                deferredNextState.resolve(newState)
            }
            return action;
        }
    }

    /**
     * dispach an action to the store and return it (eventually after it is transformed by middlewares)
     * An action can be of Any type
     */
    override var dispatch: (action: Any) -> Any = { action ->
        mainDispatcher.dispatch(this, NullDispatcher(), action)
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

