package com.brianegan.bansa.rx

import com.brianegan.bansa.Reducer
import com.brianegan.bansa.Store
import rx.Observable
import rx.subjects.PublishSubject
import rx.subjects.SerializedSubject
import rx.subscriptions.CompositeSubscription

class RxStore<S>(
        override var state: S,
        val reducer: Reducer<S>,
        /**
         * in android we need to keep track of all rx.Subscriptions and unsubscribe on Activity.onDestroy or Fragment.onDestroyView.
         * A common practice is put all subscription in a CompositeSubscription that can be unsubscribed with a single call.
         * We can optionally pass to [RxStore] constructor such CompositeSubscription so that store subscribe/unsubscribe can handle it
         */
        val allRxSubscriptions: CompositeSubscription?=null
) : Store<S> {

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
     * note: subscribe take as input an [rx.Subscriber] which is a base class for [RxStoreSubscriber]
     * The only advantage of using RxStoreSubscriber is that we avoid the need to override all onNext,onCompleted,onError methods
     * we just need to override [RxStoreSubscriber.onStateChange] that is binded to onNext
     */
    fun subscribe(subscriber: rx.Subscriber<S>): RxStoreSubscription<S> {
        return RxStoreSubscription(this,subscriber)
    }
}
