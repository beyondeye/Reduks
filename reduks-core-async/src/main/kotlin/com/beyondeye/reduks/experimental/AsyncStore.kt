package com.beyondeye.reduks.experimental

//import android.util.Log
import com.beyondeye.reduks.*
import com.beyondeye.reduks.experimental.middlewares.AsyncActionMiddleWare
import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.experimental.DefaultDispatcher
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
//import kotlinx.coroutines.experimental.newSingleThreadContext
import kotlin.coroutines.experimental.CoroutineContext


/**
 * Store that use kotlin coroutine channels for notifying asynchronously to store subscribers about
 * state changes.
 * By default the subscribeContext (the coroutine context used by subscribers when notified of store changes
 * is the Android UI thread, because usually subscribers need to update views according to state changes
 * More in general you can use any single thread context, for example:
 * val subscribeContext=newSingleThreadContext("SubscribeThread")
 */
//
class AsyncStore<S>(initialState: S, private var reducer: Reducer<S>,
                    subscribeContext: CoroutineContext,
                    reduceContext: CoroutineContext =DefaultDispatcher
                    ) : Store<S> {
    class Creator<S>(
                     val subscribeContext: CoroutineContext,
                     val reduceContext: CoroutineContext =DefaultDispatcher,
                     val withStandardMiddleware:Boolean=true) : StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            val res = AsyncStore<S>(initialState, reducer,subscribeContext,reduceContext)
            return if (!withStandardMiddleware)
                res
            else
                res.applyMiddleware(ThunkMiddleware(),AsyncActionMiddleWare())
        }
    }
    override var state:S=initialState
            private set
    private var subscribers= listOf<StoreSubscriber<S>>()
    private val subscriberNotifierActor: SendChannel<S>
    private val reducerActor: SendChannel<Any>
    init {
        subscriberNotifierActor = startSubscriberNotifierActor(subscribeContext)
        reducerActor = startReducerActor(subscriberNotifierActor,reduceContext)
    }
    //NOTE THAT IF THE ObserveContext is a single thread(the ui thread)
    // then subscribers will be notified sequentially of state changes in the correct
    private fun startSubscriberNotifierActor(c: CoroutineContext) = actor<S>(c) {
        for(updatedState in channel) { //iterate over incoming state updates
            notifySubscribers()
        }
    }
    private fun startReducerActor(subscriberNotifierChannel:SendChannel<S>,c: CoroutineContext =DefaultDispatcher)= actor<Any>(c) {
        for(action in channel) { //iterate over incoming actions
            var newState=state
            try {
                newState = reducer.reduce(state, action) //return newState
            } catch (e:Exception) {
                //Log.w("rdks","exception in reducer while processing $action: ${e.toString()}")
            }
            state=newState
            subscriberNotifierChannel.send(newState)

        }
    }

    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer = reducer
        dispatch(INIT())
    }

    private val mainDispatcher = object : Middleware<S> {
        override fun dispatch(store: Store<S>, nextDispatcher:  (Any)->Any, action: Any): Any {
            launch(DefaultDispatcher) {
                reducerActor.send(action)
            }
            return action
        }
    }

    private fun notifySubscribers() {
        subscribers.forEach { sub->
            try {
                sub.onStateChange()
            } catch(e:Exception) {
               // Log.w(SimpleStore.redukstag,"exception while notifying state change to subscriber: ${e.toString()}")
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
    fun stopActors() {
        subscriberNotifierActor.close()
        reducerActor.close()
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

}

