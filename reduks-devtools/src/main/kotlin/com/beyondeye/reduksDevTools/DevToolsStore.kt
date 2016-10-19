package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.applyMiddleware

class DevToolsStore<S>
@SafeVarargs
constructor(initialState: S, reducer: Reducer<S>, vararg middlewares: Middleware<S>) : Store<S> {
    override fun replaceReducer(reducer: Reducer<S>) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    class Creator<S> : StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> = DevToolsStore<S>(initialState,reducer)
    }
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
    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        return devToolsStore.subscribe(StoreSubscriberFn<DevToolsState<S>> {  storeSubscriber.onStateChange() })
    }
}
