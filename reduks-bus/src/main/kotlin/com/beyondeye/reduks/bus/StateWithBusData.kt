package com.beyondeye.reduks.bus

/**
 * base interface for reduks State class that can handle bus data
 * NOTE: it would be much easier to to have StateWithBusData be an abstract Class, so that
 *       there would be no need to override busData with a concrete implementation, but since
 *       data classes cannot inherit except from interfaces (at least in kotlin 1.0.x), this
 *       choice would forbid us to use data classes as state with busdata
 * Created by daely on 9/30/2016.
 */
interface StateWithBusData {
    /**
     * a persistent (immutable) map that contains a key for each bus data: Simply override it with
     * override val busData:BusData = BusData.empty
     */
    val busData: BusData

    /**
     * a method that returns a copy of the state, wuth the busData map substituted with the one
     * in [newBusData].If your State class is defined as a data class, simply implement this as
     *     override fun copyWithNewBusData(newBusData: BusData) = copy(busData=newBusData)
     */
    fun copyWithNewBusData(newBusData: BusData): StateWithBusData
}


