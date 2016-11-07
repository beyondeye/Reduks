package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.AsyncActionMiddleWare
import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware
import nl.komponents.kovenant.*
import nl.komponents.kovenant.android.androidUiDispatcher
import nl.komponents.kovenant.ui.promiseOnUi

infix fun <V, R> Promise<V, Exception>.thenUi(bind: (V) -> R): Promise<R, Exception> {
    return this.then { promiseOnUi { bind(it) }.get() }
}

/**
 * Store that use kovenant promises for synchronizing action dispatches and notification to store subscribers
 */
class KovenantStore<S>(initialState: S, private var reducer: Reducer<S>, val observeOnUiThread: Boolean = true) : Store<S> {
    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer = reducer
        dispatch(INIT())
    }
    class Creator<S>(val observeOnUiThread: Boolean = true, val withStandardMiddleware:Boolean=true) : StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            val res = KovenantStore<S>(initialState, reducer, observeOnUiThread)
            return if (!withStandardMiddleware)
                res
            else
                res.applyMiddleware(ThunkMiddleware(), AsyncActionMiddleWare())
        }
    }

    val observeContext =
            if (!observeOnUiThread) {
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
    private val subscribers= mutableListOf<StoreSubscriber<S> >()
    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any {
            val deferredNextState = deferred<S, Exception>()
            var curStatePromise: Promise<S, Exception>? = null
            synchronized(this) {
                curStatePromise = _statePromise
                _statePromise = deferredNextState.promise
            }
            if (observeOnUiThread) {
                curStatePromise?.then(defaultContext) { startState ->
                    val newState = reducer.reduce(startState, action) //return newState
                    //NOTE THAT IF THE ObserveContext is a single thread(the ui thread)
                    // then subscribers will be notified sequentially of state changes in the correct
                    // order even if we resolve the newState promise here.
                    // If we would wait to resolve the newstate promise  after subscriber notification we risk to cause deadlocks
                    deferredNextState.resolve(newState)
                    newState
                }?.then(observeContext) { newState ->
                    notifySubscribers()
                }
            } else {
                curStatePromise?.then(defaultContext) { startState ->
                    reducer.reduce(startState, action) //return newState
                }?.then(observeContext) { newState ->
                    deferredNextState.resolve(newState) //subscriber would not be able to access the newState if we not resolve the promise
                    notifySubscribers()
                }

            }
            return action
        }
    }

    private fun notifySubscribers() {
        for (i in subscribers.indices) {
            subscribers[i].onStateChange()
        }
    }

    /**
     * dispach an action to the store and return it (eventually after it is transformed by middlewares)
     * An action can be of Any type
     */
    override var dispatch: (action: Any) -> Any = { action ->
        mainDispatcher.dispatch(this,
                 { it -> it}, //null dispatcher that ends the chain
                action)
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

