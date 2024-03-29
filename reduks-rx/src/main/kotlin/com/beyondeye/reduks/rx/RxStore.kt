package com.beyondeye.reduks.rx

import com.beyondeye.reduks.*
import com.beyondeye.reduks.middlewares.ThunkMiddleware
import com.beyondeye.reduks.middlewares.applyMiddleware
import rx.Observable
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subscriptions.CompositeSubscription

class RxStore<S>(
        override var state: S,
        private var reducer: Reducer<S>,
        /**
         * in android we need to keep track of all rx.Subscriptions and unsubscribe on Activity.onDestroy or Fragment.onDestroyView.
         * A common practice is put all subscription in a CompositeSubscription that can be unsubscribed with a single call.
         * We can optionally pass to [RxStore] constructor such CompositeSubscription so that store subscribe/unsubscribe can handle it
         */
        val allRxSubscriptions: CompositeSubscription?=null
) : Store<S> {
    override fun replaceReducer(reducer: Reducer<S>) {
        this.reducer=reducer
        dispatch(INIT())
    }

    //TODO: actually I have not added yet support to reporting internal errors in RxStore, because I am planning to remove RxStore from reduks in the next major version
    override var errorLogFn: ((String) -> Unit)?=null

    class Creator<S>(val allRxSubscriptions: CompositeSubscription?=null, val withStandardMiddlewares:Boolean=true) : StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            val res = RxStore<S>(initialState, reducer, allRxSubscriptions)
            return if (!withStandardMiddlewares)
                res
            else
                res.applyMiddleware(ThunkMiddleware())
        }
    }
    val stateChanges: Observable<S>
    private val dispatcher = SerializedSubject<Any, Any>(PublishSubject.create<Any>()) //Any: is the typo of an Action


    init {
        stateChanges = dispatcher // When an action is dispatched
                .scan(state, { state, action -> reducer.reduce(state, action) }) // Run the action through your reducers, producing a new state
                .doOnNext { newState -> state = newState } // Update the state field of the instance for lazy access
                .share() // Share the Observable so all subscribers receive the same values

        stateChanges.subscribe()
    }

    override var dispatch: (action: Any) -> Any = { action ->
        dispatcher.onNext(action)
        action
    }

    /**
     * note: subscribe method that take as input an rx.Subscriber which is a base class for RxStoreSubscriber
     * The only advantage of using RxStoreSubscriber is that we avoid the need to override all onNext,onCompleted,onError methods
     * we just need to override RxStoreSubscriber.onStateChange that is binded to onNext
     */
    fun subscribeRx(subscriber: rx.Subscriber<S>, observeOnAndroidMainThread:Boolean=true): RxStoreSubscription<S> {
        return RxStoreSubscription(this,subscriber, observeOnAndroidMainThread)
    }
    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        if(storeSubscriber !is RxStoreSubscriber)
            throw IllegalArgumentException("wrong subscriber type")
        return subscribeRx(storeSubscriber)
    }

}
