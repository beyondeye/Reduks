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
    }
    override fun enhance(next: StoreCreator<S>): StoreCreator<S> = BusEnhancerStoreCreator<S>(next)
}

internal fun StateWithBusData.newStateWithUpdatedBusData(key: String, newData: Any): StateWithBusData {
    return copyWithNewBusData(busData.plus(key,newData))
}

internal fun StateWithBusData.newStateWithRemovedBusData(key: String): StateWithBusData {
    return copyWithNewBusData(busData.minus(key))
}


internal object NullData
internal fun <S : StateWithBusData, BusDataType> getStoreSubscriberBuilderForBusDataHandler(key: String, fn: (bd: BusDataType?) -> Unit) = StoreSubscriberBuilderFn<S> { store ->
    val selector = SelectorBuilder<S>()
    val busDataSel = selector.withSingleField { busData[key] ?: NullData }
    StoreSubscriberFn {
        val newState = store.state
        busDataSel.onChangeIn(newState) { optNewBusData ->
            val busDataVal = if (optNewBusData is NullData)
                null
            else
                @Suppress("UNCHECKED_CAST") (optNewBusData as? BusDataType)
            fn(busDataVal)
        }
    }
}

