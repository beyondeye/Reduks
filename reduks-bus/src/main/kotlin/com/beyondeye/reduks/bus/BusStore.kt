package com.beyondeye.reduks.bus

import com.beyondeye.reduks.*

/**
 * Created by daely on 10/6/2016.
 */
/**
 *
 */
class BusStore<S: StateWithBusData>(val wrappedStore: Store<S>, reducer: Reducer<S>) : Store<S> {
    init {
        wrappedStore.replaceReducer(combineReducers(reducer, getBusReducer()))
    }
    override val state: S get() = wrappedStore.state
    override var dispatch: (Any) -> Any
        get() = wrappedStore.dispatch
        set(value) { wrappedStore.dispatch=value }
    fun removeAllBusDataHandlers() {
        busDataHandlerSubscriptions.forEach { it.unsubscribe() }
        busDataHandlerSubscriptions.clear()
    }
    internal val busDataHandlerSubscriptions:MutableList<StoreSubscription> = mutableListOf()
    /**
     * this function name has _busstore suffix for avoid clash with Store.addBusDataHandler in function inlining
     */
    fun <BusDataType> addBusDataHandler_busstore(key:String, fn: (bd: BusDataType?) -> Unit): StoreSubscription {
        val sub=wrappedStore.subscribe(getStoreSubscriberBuilderForBusDataHandler<S,BusDataType>(key,fn))
        busDataHandlerSubscriptions.add(sub!!)
        return sub
    }
    fun removeBusDataHandler(subscription: StoreSubscription) {
        subscription.unsubscribe()
        busDataHandlerSubscriptions.remove(subscription)
    }
    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        return wrappedStore.subscribe(storeSubscriber)
    }
    override fun replaceReducer(reducer: Reducer<S>) {
        wrappedStore.replaceReducer(combineReducers(reducer, getBusReducer()))
    }
}
val <S:StateWithBusData> BusStore<S>.nSubscriptions:Int get()=busDataHandlerSubscriptions.size
