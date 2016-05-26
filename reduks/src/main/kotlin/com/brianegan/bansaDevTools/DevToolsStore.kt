package com.brianegan.bansaDevTools

import com.brianegan.bansa.Middleware
import com.brianegan.bansa.Reducer
import com.brianegan.bansa.SimpleStore
import com.brianegan.bansa.Store
import com.brianegan.bansa.StoreSubscriber
import com.brianegan.bansa.StoreSubscription
import com.brianegan.bansa.middlewares.applyMiddleware

class DevToolsStore<S>
@SafeVarargs
constructor(initialState: S, reducer: Reducer<S>, vararg middlewares: Middleware<S>) : Store<S> {
    private val devToolsStore: SimpleStore<DevToolsState<S>>

    init {
        val devToolsState = DevToolsState(
                listOf(initialState),
                emptyList<Any>(),
                0)

        val devToolsReducer = DevToolsReducer(reducer)

        devToolsStore = SimpleStore(devToolsState, devToolsReducer)
        devToolsStore.applyMiddleware(*toDevToolsMiddlewares(middlewares))
        devToolsStore.dispatch(DevToolsAction.createInitAction())
    }

    private fun toDevToolsMiddlewares(middlewares: Array<out Middleware<S>>): Array<Middleware<DevToolsState<S>>> {
        val devToolsMiddlewares = Array<Middleware<DevToolsState<S>>>(middlewares.size) {
            DevToolsMiddleware(this, middlewares[it])
        }
        return devToolsMiddlewares
    }

    val devToolsState: DevToolsState<S>
        get() = devToolsStore.state

    override val state: S
        get() = devToolsStore.state.currentAppState

    override var dispatch: (action: Any) -> Any ={ action ->
        if (action is DevToolsAction) {
            devToolsStore.dispatch(action)
        } else {
            devToolsStore.dispatch(DevToolsAction.createPerformAction(action))
        }
    }
    fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        return devToolsStore.subscribe(StoreSubscriber<DevToolsState<S>> { devstate -> storeSubscriber.onStateChange(state) })
    }
}
