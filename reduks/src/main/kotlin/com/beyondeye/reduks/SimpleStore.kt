package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware

class SimpleStore<S>(initialState: S, private var reducer: Reducer<S>) : Store<S> {
    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer=reducer
        dispatch(INIT())
    }
    class Creator<S>(val withStandardMiddlewares:Boolean=true): StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
          val res=SimpleStore<S>(initialState,reducer)
            return if(!withStandardMiddlewares)
                res
            else
                res.applyMiddleware(ThunkMiddleware())
        }
    }

    override var errorLogFn: ((String) -> Unit)?=null
    override var state: S = initialState
    private val subscribers = mutableListOf<StoreSubscriber<S>>()
    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, nextDispatcher: (Any) -> Any, action: Any): Any {
            synchronized(this) {
                try {
                    state = reducer.reduce(store.state, action)
                } catch (e: Throwable) {
                    ReduksInternalLogUtils.reportErrorInReducer(this@SimpleStore, e)
                }

                subscribers.forEach {
                    try {
                        it.onStateChange()
                    } catch (e:Throwable) {
                        ReduksInternalLogUtils.reportErrorInSubscriber(this@SimpleStore,e)
                    }
                }
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
    companion object {
        val redukstag = "rdks"
    }
}

