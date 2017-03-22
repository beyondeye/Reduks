package com.beyondeye.reduks.bus

import com.beyondeye.reduks.*

/**
 * the only real reason we need a BusStore, and we cannot implement the BusStore functionality like FragmentStatus boils
 * down to the type info on state that is encapsulated in BusStore, that allows us to call addBusDataHandler without the
 * need to explicitely pass also the state type
 * Created by daely on 10/6/2016.
 */
class BusStore<S: StateWithBusData>(val wrappedStore: Store<S>, reducer: Reducer<S>) : Store<S> {
    init {
        wrappedStore.replaceReducer(combineReducers(reducer, busDataReducer()))
    }
    override val state: S get() = wrappedStore.state
    override var dispatch: (Any) -> Any
        get() = wrappedStore.dispatch
        set(value) { wrappedStore.dispatch=value }
    /**
     * this function name has _busstore suffix for avoid clash with Store.addBusDataHandler in function inlining
     */
    fun <BusDataType> addBusDataHandler_busstore(key:String, fn: (bd: BusDataType?) -> Unit): StoreSubscription {
        val sub=wrappedStore.subscribe(getStoreSubscriberBuilderForBusDataHandler<S,BusDataType>(key,fn))
        return sub!!
    }
    fun removeBusDataHandler(subscription: StoreSubscription) {
        subscription.unsubscribe()
    }
    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        return wrappedStore.subscribe(storeSubscriber)
    }
    override fun replaceReducer(reducer: Reducer<S>) {
        wrappedStore.replaceReducer(combineReducers(reducer, busDataReducer()))
    }
}
