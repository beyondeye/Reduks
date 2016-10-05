package com.beyondeye.reduks.bus

import com.beyondeye.reduks.*
import com.beyondeye.reduks.pcollections.HashTreePMap
import com.beyondeye.reduks.pcollections.PMap

fun emptyBusData():PMap<String,Any> =  HashTreePMap.empty()

/**
 * Created by daely on 9/30/2016.
 */
interface StateWithBusData {
    val busData:PMap<String,Any>
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
//TODO add Opt class in selectors file and add documentation for it
class Opt<T>(val it:T?)
internal fun <S : StateWithBusData, BusDataType> getStoreSubscriberBuilderForBusDataHandler(key: String, fn: (bd: BusDataType) -> Unit) = StoreSubscriberBuilderFn<S> { store ->
    val selector = SelectorBuilder<S>()
    var lastVal:BusDataType?=null
    val busDataSel = selector.withField { busData }.compute { bd ->
        @Suppress("UNCHECKED_CAST") Opt(bd[key] as BusDataType?) }
    StoreSubscriberFn {
        val newState = store.state
        busDataSel.onChangeIn(newState) { optNewVal->
            if(optNewVal.it!=null) {
                val newVal=optNewVal.it
                if(newVal!==lastVal) { //unfortunately, selectors alone cannot catch change of busData but no change to a specific key TODO try to think of a better solution
                    fn(newVal)
                    lastVal=newVal
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
    val busDataHandlerSubscriptions:MutableList<StoreSubscription> = mutableListOf()
    fun <BusDataType> addBusDataHandler(key:String, fn: (bd: BusDataType) -> Unit): StoreSubscription {
        val sub=wrappedStore.subscribe(getStoreSubscriberBuilderForBusDataHandler<S,BusDataType>(key,fn))
        busDataHandlerSubscriptions.add(sub)
        return sub
    }
    override fun subscribe(storeSubscriber: StoreSubscriber<S>): StoreSubscription {
        return wrappedStore.subscribe(storeSubscriber)
    }
    override fun replaceReducer(reducer: Reducer<S>) {
        wrappedStore.replaceReducer(combineReducers(reducer, getBusReducer()))
    }

}
inline fun <S,reified BusDataType:Any> Store<S>.busData(key:String?=null):BusDataType? {
    if(this !is BusStore<*>) return null
    return this.state.busData[key?: BusDataType::class.java.name] as BusDataType?
}
inline fun <S,reified BusDataType:Any> Store<S>.clearBusData(key:String?=null) {dispatch(ActionClearBusData(key?: BusDataType::class.java.name)) }
fun <S, BusDataType :Any> Store<S>.postBusData(data: BusDataType, key:String?=null) { dispatch(ActionSendBusData(key ?: data.javaClass.name,data)) }
fun <S> Store<S>.unsubscribeAllBusDataHandlers() {
    if(this is BusStore<*>) this.unsubscribeAllBusDataHandlers()
}
inline fun <S,reified BusDataType:Any> Store<S>.addBusDataHandler(key:String?=null, noinline fn: (bd: BusDataType?) -> Unit) :StoreSubscription?{
    if(this is BusStore<*>) return this.addBusDataHandler<BusDataType>(key?: BusDataType::class.java.name,fn)
    else return null
}

/**
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
            throw NotImplementedError()
        }
    }
    override fun enhance(next: StoreCreator<S>): StoreCreator<S> = BusEnhancerStoreCreator<S>(next)
}