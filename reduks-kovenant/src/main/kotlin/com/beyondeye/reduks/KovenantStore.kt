package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.AsyncActionMiddleWare
import com.beyondeye.reduks.middlewares.ThunkMiddleware
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
class KovenantStore<S>(initialState: S, reducer_: Reducer<S>, val observeOnUiThread: Boolean = true) : Store<S> {
    var reducer:Reducer<S> = reducer_
        private set
    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer=reducer
    }
    class Factory<S>( val observeOnUiThread: Boolean = true) : StoreFactory<S> {
        override fun newStore(initialState: S, reducer: Reducer<S>): Store<S> = KovenantStore<S>(initialState,reducer,observeOnUiThread)
        override val storeStandardMiddlewares:Array<Middleware<S>> = arrayOf(ThunkMiddleware<S>(),AsyncActionMiddleWare<S>())
        override fun <S_> ofType(): StoreFactory<S_> {
            return Factory<S_>(observeOnUiThread)
        }
    }
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
            if(observeOnUiThread) {
                curStatePromise?.then(defaultContext) { startState ->
                    val newState = reducer.reduce(startState, action) //return newState
                    //NOTE THAT IF THE ObserveContext is a single thread(the ui thread)
                    // then subscribers will be notified sequentially of state changes in the correct
                    // order even if we resolve the newState promise here.
                    // If we would wait to resolve the newstate promise  after subscriber notification we risk to cause deadlocks
                    deferredNextState.resolve(newState)
                    newState
                }?.then(observeContext) { newState ->
                    notifySubscribers(newState)
                }
            } else
            { //in case we don't observe on the ui thread, then the correct thing to do, for
              //being sure that subscribers always sees the sequence of state changes as they
              //actually happened is to wait to resolve the state change promise after the notification phase
                curStatePromise?.then(defaultContext) { startState ->
                    reducer.reduce(startState, action) //return newState
                }?.then(observeContext) { newState ->
                    notifySubscribers(newState)
                    deferredNextState.resolve(newState)
                }

            }
            return action
        }
    }
    private fun notifySubscribers(newState:S) {
        for (i in subscribers.indices) {
            subscribers[i].onStateChange(newState)
        }
    }

    /**
     * dispach an action to the store and return it (eventually after it is transformed by middlewares)
     * An action can be of Any type
     */
    override var dispatch: (action: Any) -> Any = { action ->
        mainDispatcher.dispatch(this, NullDispatcher(), action)
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

