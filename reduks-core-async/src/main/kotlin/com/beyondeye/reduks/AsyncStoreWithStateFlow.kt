package com.beyondeye.reduks

import com.beyondeye.reduks.middlewares.AsyncActionMiddleWare
import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.*
//import kotlinx.coroutines.experimental.newSingleThreadContext

/**
 * Store that use kotlin coroutine channels for notifying asynchronously to store subscribers about
 * state changes.
 * By default the subscribeDispatcher (the coroutine context used by subscribers when notified of store changes
 * is the Android UI thread, because usually subscribers need to update views according to state changes
 * More in general you can use any single thread context, for example:
 * val subscribeDispatcher=newSingleThreadContext("SubscribeThread")
 */
//
class AsyncStoreWithStateFlow<S>(initialState: S, private var reducer: Reducer<S>,
                                 val cscope: CoroutineScope,
                                 subscribeDispatcher: CoroutineDispatcher,
                                 val reduceDispatcher: CoroutineDispatcher =Dispatchers.Default
                    ) : Store<S> {
    class Creator<S>(
            val cscope: CoroutineScope,
            val subscribeDispatcher: CoroutineDispatcher,
            val reduceDispatcher: CoroutineDispatcher =Dispatchers.Default,
            val withStandardMiddleware:Boolean=true) : StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            val res = AsyncStoreWithStateFlow<S>(initialState, reducer,cscope,subscribeDispatcher,reduceDispatcher)
            return if (!withStandardMiddleware)
                res
            else
                res.applyMiddleware(ThunkMiddleware(), AsyncActionMiddleWare())
        }
    }

    override var errorLogFn: ((String) -> Unit)?=null
    private var deferredState: Deferred<S> = cscope.async { initialState }
    override val state:S
        get() = runBlocking(reduceDispatcher) {
            deferredState.await()
        }
    private var subscribers= listOf<StoreSubscriber<S>>()
    init {
    }

    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer = reducer
        dispatch(INIT())
    }

    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any {
            //get a reference of current deferred so that we make sure that all reduce action are actually executed in the correct order
            val curDeferredState = deferredState
            //update deferredState with result of async job of running the reducer
            deferredState = cscope.async(reduceDispatcher) {
                val startState = curDeferredState.await()
                val newState = try {
                    reducer.reduce(startState, action) //return newState
                } catch (e: Throwable) {
                    ReduksInternalLogUtils.reportErrorInReducer(this@AsyncStoreWithStateFlow, e)
                    startState
                }
                newState
            }
            //after creating the new deferredState, handle notification of subscribers once this new
            //deferredState is resolved
            val nextDeferredState=deferredState
            cscope.launch(reduceDispatcher) {
                nextDeferredState.await()
                //NOTE THAT IF THE ObserveContext is a single thread(the ui thread)
                // then subscribers will be notified sequentially of state changes in the correct order
                withContext(subscribeDispatcher) {
                    notifySubscribers()
                }
            }

            return action
        }
    }

    private fun notifySubscribers() {
        subscribers.forEach { sub->
            try {
                sub.onStateChange()
            } catch(e:Exception) {
                ReduksInternalLogUtils.reportErrorInSubscriber(this,e)
            }
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
        synchronized(subscribers) {
            subscribers= subscribers.plus(storeSubscriber)
        }
        return object : StoreSubscription {
            override fun unsubscribe() {
                synchronized(subscribers) {
                    subscribers= subscribers.minus(storeSubscriber)
                }
            }
        }
    }

    /**
     * this method does nothing: we don't use actors in the current version of asyncstore
     */
    fun stopActors() {

    }

}

