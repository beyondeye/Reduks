package com.beyondeye.reduks.bus

import com.beyondeye.reduks.*
import java.lang.ref.WeakReference

/**
 * A store enhancer that enable something similar to an event bus backed by actual reduks action/reducers/subscribers
 * Created by daely on 9/30/2016.
 */
class BusStoreEnhancer<S: StateWithBusData> : StoreEnhancer<S>{
    internal class BusEnhancerStoreCreator<S: StateWithBusData>(val next: StoreCreator<S>) :StoreCreator<S> {
        override fun create(reducer: Reducer<S>, initialState: S): Store<S> {
            return BusStore(next.create(reducer,initialState),reducer)
        }


        override fun <S_> ofType(): StoreCreator<S_> {
            throw NotImplementedError("Don't use a BusStoreEnhancer on a single module: use it on the resulting multimodule!") //don't know how to implement this, this is mainly used in reduks modules, but I can wrap the multimodule with the store enhancer so not much of a problem here
        }
    }
    override fun enhance(next: StoreCreator<S>): StoreCreator<S> = BusEnhancerStoreCreator<S>(next)
}

internal fun StateWithBusData.newStateWithUpdatedBusData(key: String, newData: Any): StateWithBusData {
    return copyWithNewBusData(busData.plus(key,newData))
}

internal fun StateWithBusData.newStateWithRemovedBusData(key: String): StateWithBusData {
    return copyWithNewBusData(busData.minus(key))
}



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

