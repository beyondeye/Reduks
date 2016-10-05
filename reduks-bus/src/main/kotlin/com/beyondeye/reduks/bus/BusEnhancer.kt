package com.beyondeye.reduks.bus

import com.beyondeye.reduks.*
import com.beyondeye.reduks.pcollections.HashTreePMap
import com.beyondeye.reduks.pcollections.PMap
import java.lang.ref.WeakReference

fun emptyBusData():PMap<String,Any> =  HashTreePMap.empty()

/**
 * base interface for reduks State class that can handle bus data
 * Created by daely on 9/30/2016.
 */
interface StateWithBusData {
    /**
     * a persistent (immutable) map that contains a key for each bus data: Simply override it with
     * override val busData:PMap<String,Any> = emptyBusData()
     */
    val busData:PMap<String,Any>

    /**
     * a method that returns a copy of the state, wuth the busData map substituted with the one
     * in [newBusData].If your State class is defined as a data class, simply implement this as
     *     override fun copyWithNewBusData(newBusData: PMap<String, Any>) = copy(busData=newBusData)
     */
    fun copyWithNewBusData(newBusData:PMap<String,Any>): StateWithBusData
}

internal fun StateWithBusData.newStateWithUpdatedBusData(key: String, newData: Any): StateWithBusData {
    return copyWithNewBusData(busData.plus(key,newData))
}

internal fun StateWithBusData.newStateWithRemovedBusData(key: String): StateWithBusData {
    return copyWithNewBusData(busData.minus(key))
}

/**
 *
 */
class ActionSendBusData(val key: String, val newData: Any)

class ActionClearBusData(val key: String)

internal fun <S : StateWithBusData> getBusReducer(): Reducer<S> {
    return ReducerFn { s, a ->
        when (a) {
            is ActionSendBusData -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithUpdatedBusData(a.key,a.newData) as S
            }
            is ActionClearBusData -> {
                @Suppress("UNCHECKED_CAST")
                s.newStateWithRemovedBusData(a.key) as S
            }
            else -> s
        }
    }
}
internal fun <S : StateWithBusData, BusDataType> getStoreSubscriberBuilderForBusDataHandler(key: String, fn: (bd: BusDataType) -> Unit) = StoreSubscriberBuilderFn<S> { store ->
    val selector = SelectorBuilder<S>()
    var lastBusData=WeakReference<BusDataType>(null) //NO need to keep a strong reference here
    val busDataSel = selector.withField { busData }.compute { bd ->
        @Suppress("UNCHECKED_CAST") Opt(bd[key] as BusDataType?) }
    StoreSubscriberFn {
        val newState = store.state
        busDataSel.onChangeIn(newState) { optNewBusData->
            optNewBusData.it?.let { newBusData ->
                if(newBusData!==lastBusData.get()) { //unfortunately, selectors alone cannot catch change of busData but no change to a specific key TODO try to think of a better solution
                    fn(newBusData)
                    lastBusData=WeakReference(newBusData)
                }
            }
        }
    }
}


/**
 *
 */
class BusStore<S: StateWithBusData>(val wrappedStore:Store<S>,  reducer: Reducer<S>) : Store<S> {
    init {
        wrappedStore.replaceReducer(combineReducers(reducer, getBusReducer()))
    }
    override val state: S get() = wrappedStore.state
    override var dispatch: (Any) -> Any
        get() = wrappedStore.dispatch
        set(value) { wrappedStore.dispatch=value }
    fun unsubscribeAllBusDataHandlers() {
        busDataHandlerSubscriptions.forEach { it.unsubscribe() }
        busDataHandlerSubscriptions.clear()
    }
    private val busDataHandlerSubscriptions:MutableList<StoreSubscription> = mutableListOf()
    fun <BusDataType> addBusDataHandler(key:String, fn: (bd: BusDataType) -> Unit): StoreSubscription {
        val sub=wrappedStore.subscribe(getStoreSubscriberBuilderForBusDataHandler<S,BusDataType>(key,fn))
        busDataHandlerSubscriptions.add(sub)
        return sub
    }
    fun removeBusDataHandler(subscription:StoreSubscription) {
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
//-------
inline fun <reified BusDataType:Any> Store<out StateWithBusData>.busData(key:String?=null):BusDataType? {
    if(this !is BusStore<*>) return null
    return this.state.busData[key?: BusDataType::class.java.name] as BusDataType?
}
inline fun <reified BusDataType:Any> Reduks<out StateWithBusData>.busData(key:String?=null):BusDataType?=store.busData(key)
//-------
inline fun <reified BusDataType:Any> Store<out StateWithBusData>.clearBusData(key:String?=null) {dispatch(ActionClearBusData(key?: BusDataType::class.java.name)) }
inline fun <reified BusDataType:Any> Reduks<out StateWithBusData>.clearBusData(key:String?=null) { store.clearBusData<BusDataType>(key) }
//-------
fun <BusDataType :Any> Store<out StateWithBusData>.postBusData(data: BusDataType, key:String?=null) { dispatch(ActionSendBusData(key ?: data.javaClass.name,data)) }
fun <BusDataType :Any> Reduks<out StateWithBusData>.postBusData(data: BusDataType, key:String?=null) { store.postBusData(data,key) }
//-------
fun Store<out StateWithBusData>.unsubscribeAllBusDataHandlers() {
    if(this is BusStore<*>) this.unsubscribeAllBusDataHandlers()
}
fun Reduks<out StateWithBusData>.unsubscribeAllBusDataHandlers() { store.unsubscribeAllBusDataHandlers()}
//--------
fun Store<out StateWithBusData>.removeBusDataHandler(subscription: StoreSubscription) {
    if(this is BusStore<*>) this.removeBusDataHandler(subscription)
}
fun Reduks<out StateWithBusData>.removeBusDataHandler(subscription: StoreSubscription) { store.removeBusDataHandler(subscription) }

fun Reduks<out StateWithBusData>.removeBusDataHandlers(subscriptions: MutableList<StoreSubscription>?) {
    subscriptions?.forEach { store.removeBusDataHandler(it) }
    subscriptions?.clear()
}

//--------
inline fun <reified BusDataType:Any> Store<out StateWithBusData>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) :StoreSubscription?{
    if(this is BusStore<*>) return this.addBusDataHandler<BusDataType>(key?: BusDataType::class.java.name,fn)
    else return null
}
inline fun <reified BusDataType:Any> Reduks<out StateWithBusData>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) :StoreSubscription?=
        store.addBusDataHandler(key,fn)
/**
 * A store enhancer that enable something similar to an event bus backed by actual reduks action/reducers/subscribers
 * Created by daely on 9/30/2016.
 */
class BusStoreEnhancer<S: StateWithBusData> : StoreEnhancer<S>{
    internal class BusEnhancerStoreCreator<S: StateWithBusData>(val next: StoreCreator<S>) :StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            return BusStore(next.create(reducer,initialState),reducer)
        }

        override val storeStandardMiddlewares: Array<out Middleware<S>>
            get() = next.storeStandardMiddlewares

        override fun <S_> ofType(): StoreCreator<S_> {
            throw NotImplementedError("Don't use a BusStoreEnhancer on a single module: use it on the resulting multimodule!") //don't know how to implement this, this is mainly used in reduks modules, but I can wrap the multimodule with the store enhancer so not much of a problem here
        }
    }
    override fun enhance(next: StoreCreator<S>): StoreCreator<S> = BusEnhancerStoreCreator<S>(next)
}