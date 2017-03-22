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
fun <S>Store<S>.getBusStore():BusStore<out StateWithBusData>? {
    if(this is MultiStore) return this.subStoreWithBus()
    @Suppress("UNCHECKED_CAST")
    return (this as? BusStore<out StateWithBusData>)

}
//-------
inline fun <reified BusDataType:Any> MultiStore.busData(key:String?=null):BusDataType? {
    return subStoreWithBus()?.busData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> Store<out Any>.busData(key:String?=null):BusDataType? {
    return getBusStore()?.busData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> BusStore<out StateWithBusData>.busData(key:String?=null):BusDataType? {
    return state.busData.get(busDataKey<BusDataType>(key)) as BusDataType?
}

inline fun <reified BusDataType:Any> Reduks<out StateWithBusData>.busData(key:String?=null):BusDataType?=store.busData(key)
//-------
inline fun <reified BusDataType:Any> MultiStore.clearBusData(key:String?=null) {
     subStoreWithBus()?.clearBusData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> Store<out Any>.clearBusData(key:String?=null) {
    getBusStore()?.clearBusData<BusDataType>(key)
}
inline fun <reified BusDataType:Any> BusStore<out StateWithBusData>.clearBusData(key:String?=null) {
    dispatch(ActionClearBusData(busDataKey<BusDataType>(key)))
}

inline fun <reified BusDataType:Any> Reduks<out Any>.clearBusData(key:String?=null) {
    store.clearBusData<BusDataType>(key)
}
//-------
inline fun <reified BusDataType :Any> MultiStore.postBusData(data: BusDataType, key:String?=null) {
    subStoreWithBus()?.postBusData(data,key)
}
inline fun <reified BusDataType :Any> Store<out Any>.postBusData(data: BusDataType, key:String?=null) {
    getBusStore()?.postBusData(data,key)
}
inline fun <reified BusDataType :Any> BusStore<out StateWithBusData>.postBusData(data: BusDataType, key:String?=null) {
    dispatch(ActionSendBusData(busDataKey<BusDataType>(key),data))
}

inline fun <reified BusDataType :Any> Reduks<out Any>.postBusData(data: BusDataType, key:String?=null) {
    store.postBusData(data,key)
}

//-------

fun Reduks<out Any>.removeAllBusDataHandlers() {
    busStoreSubscriptionsByTag.keys.forEach {tag -> removeBusDataHandlers(tag) }
    busStoreSubscriptionsByTag.clear()
}


//---------
fun Reduks<out Any>.removeBusDataHandlers(subscriptionTag: String) {
    busStoreSubscriptionsByTag[subscriptionTag]?.let { subs->
        subs.forEach { it.unsubscribe() }
        subs.clear()
    }
}
//--------
inline fun <reified BusDataType:Any> MultiStore.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? {
    return subStoreWithBus()?.addBusDataHandler(key,fn)
}

inline fun <reified BusDataType:Any> Store<out Any>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription?{
    return getBusStore()?.addBusDataHandler(key,fn)
}

inline fun <reified BusDataType:Any> BusStore<out StateWithBusData>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription?{
    return addBusDataHandler_busstore<BusDataType>(this.busDataKey<BusDataType>(key),fn)
}

/**
 * [addBusDataHandler] with option to automatically clear bus data after handling
 */
inline fun <reified BusDataType:Any> Reduks<out Any>.AddBusDataHandler(subscriptionTag:String,clearBusDataAfterHandling:Boolean,key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription? {
    val fn_ = if (!clearBusDataAfterHandling) fn else
        { bd ->
            fn(bd)
            if(bd!=null) clearBusData<BusDataType>(key)
        }
    return addBusDataHandler(subscriptionTag,key,fn_)
}

/**
 * add a bus data handler with the specified tag, so that it can be later be removed with [removeBusDataHandlers]
 * Note that multiple Bus data handlers with the same tag are possible
 * Note also that while [key] is used for linking the handler to some specific BusDataType to be handled, [subscriptionTag] is used for grouping
 * together handlers so that it is possible to remove all at once with Reduks.removeBusDataHandlers
 */
inline fun <reified BusDataType:Any> Reduks<out Any>. addBusDataHandler(subscriptionTag:String,key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) : StoreSubscription?
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

