package com.beyondeye.reduks.bus

import com.beyondeye.reduks.Reduks
import com.beyondeye.reduks.Store
import com.beyondeye.reduks.StoreSubscription
import com.beyondeye.reduks.modules.MultiStore
//--------------------
//some basic utility extension methods
inline fun <reified BusDataType:Any> Store<out Any>.busDataKey(key:String?=null):String =key?:BusDataType::class.java.name
inline fun <reified BusDataType:Any> Reduks<out Any>.busDataKey(key:String?=null):String =store.busDataKey<BusDataType>(key)

/**
 * for multistore, use one of the substores as bus store
 */
fun  MultiStore.subStoreWithBus():BusStore<out StateWithBusData>?{
    @Suppress("UNCHECKED_CAST")
    return this.storeMap.values.firstOrNull() { it is BusStore } as? BusStore<out StateWithBusData>
}
//-------
inline fun <reified BusDataType:Any> MultiStore.busData(key:String?=null):BusDataType? {
    return subStoreWithBus()?.busData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> BusStore<out StateWithBusData>.busData(key:String?=null):BusDataType? {
    return state.busData.get(busDataKey<BusDataType>(key)) as BusDataType?
}
inline fun <reified BusDataType:Any> Store<out Any>.busData(key:String?=null):BusDataType? {
    val multi=(this as? MultiStore)?.busData<BusDataType>(key)
    return multi ?: (this as? BusStore<*>)?.busData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> Reduks<out StateWithBusData>.busData(key:String?=null):BusDataType?=store.busData(key)

//-------
inline fun <reified BusDataType:Any> MultiStore.clearBusData(key:String?=null) {
    val s=subStoreWithBus()
    s?.clearBusData<BusDataType>(s.busDataKey<BusDataType>(key))
}
inline fun <reified BusDataType:Any> BusStore<out StateWithBusData>.clearBusData(key:String?=null) {
    dispatch(ActionClearBusData(busDataKey<BusDataType>(key)))
}

inline fun <reified BusDataType:Any> Store<out Any>.clearBusData(key:String?=null) {
    if(this is MultiStore)
        (this as MultiStore).clearBusData<BusDataType>(key)
    else
        (this as? BusStore<*>)?.clearBusData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> Reduks<out Any>.clearBusData(key:String?=null) {
    store.clearBusData<BusDataType>(store.busDataKey<BusDataType>(key))
}

//-------
inline fun <reified BusDataType :Any> MultiStore.postBusData(data: BusDataType, key:String?=null) {
    val s=subStoreWithBus()
    s?.postBusData(data,s.busDataKey<BusDataType>(key))
}
inline fun <reified BusDataType :Any> BusStore<out StateWithBusData>.postBusData(data: BusDataType, key:String?=null) {
    dispatch(ActionSendBusData(busDataKey<BusDataType>(key),data))
}

inline fun <reified BusDataType :Any> Store<out Any>.postBusData(data: BusDataType, key:String?=null) {
    if(this is MultiStore)
        (this as MultiStore).postBusData(data,key)
    else
        (this as? BusStore<*>)?.postBusData(data,key)
}
inline fun <reified BusDataType :Any> Reduks<out Any>.postBusData(data: BusDataType, key:String?=null) {
    store.postBusData(data,store.busDataKey<BusDataType>(key))
}

//-------
fun MultiStore.removeAllBusDataHandlers() {
    subStoreWithBus()?.removeAllBusDataHandlers()
}
fun Store<out Any>.removeAllBusDataHandlers() {
    if(this is MultiStore)
        (this as MultiStore).removeAllBusDataHandlers()
    else
        (this as? BusStore<*>)?.removeAllBusDataHandlers()
}
fun Reduks<out Any>.removeAllBusDataHandlers() {
    store.removeAllBusDataHandlers()
    busStoreSubscriptionsByTag.clear()
}


//--------
fun MultiStore.removeBusDataHandler(subscription: StoreSubscription) {
    subStoreWithBus()?.removeBusDataHandler(subscription)
}
fun Store<out Any>.removeBusDataHandler(subscription: StoreSubscription) {
    if(this is MultiStore)
        (this as MultiStore).removeBusDataHandler(subscription)
    else
        (this as? BusStore<*>)?.removeBusDataHandler(subscription)
}
fun Reduks<out Any>.removeBusDataHandler(subscription: StoreSubscription) {
    store.removeBusDataHandler(subscription)
}

//---------
fun Reduks<out Any>.removeBusDataHandlersWithTag(subscriptionTag: String) {
    busStoreSubscriptionsByTag[subscriptionTag]?.let { subs->
        subs.forEach { store.removeBusDataHandler(it) }
        subs.clear()
    }
}

fun Reduks<out Any>.removeBusDataHandlers(subscriptions: MutableList<StoreSubscription>?) {
    subscriptions?.forEach { store.removeBusDataHandler(it) }
    subscriptions?.clear()
}
//--------
inline fun <reified BusDataType:Any> MultiStore.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? {
    val s=subStoreWithBus()
    return s?.addBusDataHandler_busstore<BusDataType>(s.busDataKey<BusDataType>(key),fn)
}

inline fun <reified BusDataType:Any> Store<out Any>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription?{
    val multi = (this as? MultiStore)?.addBusDataHandler<BusDataType>(busDataKey<BusDataType>(key),fn)
    return multi?: (this as? BusStore<*>)?.addBusDataHandler_busstore<BusDataType>(busDataKey<BusDataType>(key),fn)
}


/**
 * this function is private for avoiding the case where user mistakingly use this function instead of addBusDataHandlerWithTag
 */
private inline fun <reified BusDataType:Any> Reduks<out Any>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription?{
   return store.addBusDataHandler(key,fn)
}

/**
 * add a bus data handler with the specified tag, so that it can be later be removed with [removeBusDataHandlersWithTag]
 * Note that multiple Bus data handlers with the same tag are possible
 * Note also that while [key] is used for linking the handler to some specific BusDataType to be handled, [subscriptionTag] is used for grouping
 * together handlers so that it is possible to remove all at once with Reduks.removeBusDataHandlersWithTag
 */
inline fun <reified BusDataType:Any> Reduks<out Any>. addBusDataHandlerWithTag(subscriptionTag:String,key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription?
{
    val handler=store.addBusDataHandler(key,fn)
    if(handler!=null) {
        var subs = busStoreSubscriptionsByTag[subscriptionTag]
        if (subs == null) {
            subs = mutableListOf<StoreSubscription>()
            busStoreSubscriptionsByTag[subscriptionTag] = subs
        }
        subs.add(handler)
    }
    return handler
}

