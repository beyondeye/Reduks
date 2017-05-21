package com.beyondeye.reduks.experimental

import android.util.Log
import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.channels.ActorJob
import kotlinx.coroutines.experimental.channels.SendChannel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext


/**
 * Store that use kotlin coroutine channels for notifying asynchronously to store subscribers about
 * state changes
 */

class AsyncStore<S>(initialState: S, private var reducer: Reducer<S>, val observeOnUiThread: Boolean = true) : Store<S> {
    class Creator<S>(val observeOnUiThread: Boolean = true, val withStandardMiddleware:Boolean=true) : StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            val res = AsyncStore<S>(initialState, reducer, observeOnUiThread)
            return if (!withStandardMiddleware)
                res
            else
                res.applyMiddleware(ThunkMiddleware())
        }
    }
    override var state:S=initialState
            private set
    private val subscribers= mutableListOf<StoreSubscriber<S>>()
    private val subscriberNotifierActor: ActorJob<S>
    private val reducerActor: ActorJob<Any>
    init {
        val subscriberContext=if(observeOnUiThread) UI else CommonPool
        subscriberNotifierActor = startSubscriberNotifierActor(subscriberContext)
        reducerActor = startReducerActor(subscriberNotifierActor.channel)
    }
    //NOTE THAT IF THE ObserveContext is a single thread(the ui thread)
    // then subscribers will be notified sequentially of state changes in the correct
    private fun startSubscriberNotifierActor(c:CoroutineContext=CommonPool) = actor<S>(c) {
        for(updatedState in channel) { //iterate over incoming state updates
            notifySubscribers()
        }
    }
    private fun startReducerActor(subscriberNotifierChannel:SendChannel<S>,c:CoroutineContext=CommonPool)= actor<Any>(c) {
        for(action in channel) { //iterate over incoming actions
            var newState=state
            try {
                newState = reducer.reduce(state, action) //return newState
            } catch (e:Exception) {
                Log.w("rdks","exception in reducer while processing $action: ${e.toString()}")
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
            launch(CommonPool) {
                reducerActor.send(action)
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
    fun stopActors() {
        subscriberNotifierActor.channel.close()
        reducerActor.channel.close()
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

