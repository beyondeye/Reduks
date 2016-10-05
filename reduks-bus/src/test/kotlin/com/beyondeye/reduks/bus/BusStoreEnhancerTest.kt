package com.beyondeye.reduks.bus

import com.beyondeye.reduks.ReducerFn
import com.beyondeye.reduks.SimpleStore
import com.beyondeye.reduks.create
import com.beyondeye.reduks.pcollections.PMap
import org.junit.Test

import org.junit.Assert.*

class Action
{
    class SetA(val newA:Int)
    class SetB(val newB:Int)
}
val testReducer = ReducerFn<TestState> { state, action ->
    when (action) {
        is Action.SetA -> state.copy(a= action.newA)
        is Action.SetB -> state.copy(b= action.newB)
        else -> state
    }
}
data class TestState(val a:Int, val b:Int, override val busData:PMap<String,Any> = emptyBusData()): StateWithBusData {
    override fun copyWithNewBusData(newBusData: PMap<String, Any>) = copy(busData=newBusData)
}
val initialState=TestState(0,0)
/**
 * Created by daely on 10/2/2016.
 */
class BusStoreEnhancerTest {
    @Test
    fun testEnhance() {
        val creator= SimpleStore.Creator<TestState>()
        val store = creator.create(testReducer, initialState,BusStoreEnhancer())
        var iDataReceivedCount:Int=0
        var fDataReceivedCount:Int=0
        store.addBusDataHandler { intVal:Int? ->
            val receivedOnBus=intVal
            if(receivedOnBus!=null) {
                iDataReceivedCount++
                assertEquals(receivedOnBus,1)
            }
        }
        store.addBusDataHandler { floatVal:Float? ->
            val receivedOnBus=floatVal
            if(receivedOnBus!=null) {
                fDataReceivedCount++
                assertEquals(receivedOnBus,2.0F)
            }
        }
        val iBusDataBeforePost:Int? =store.busData()
        assertNull(iBusDataBeforePost)
        val fBusDataBeforePost:Float? =store.busData()
        assertNull(fBusDataBeforePost)

        store.postBusData(2.0F)
        assert(fDataReceivedCount==1)
        assert(iDataReceivedCount==0)
        store.postBusData(1)
        assert(iDataReceivedCount==1)
        assert(fDataReceivedCount==1)
        val ibusData:Int?=store.busData()
        assertNotNull(ibusData)
        assertEquals(ibusData ,1)

        val fbusDataInState:Float?=store.busData()
        assertNotNull(fbusDataInState)
        assertEquals(fbusDataInState ,2.0f)

        assertTrue(iDataReceivedCount==1)
        store.clearBusData<TestState,Int>()
        assertNull(store.busData<TestState,Int>())
        store.clearBusData<TestState,Float>()
        assertNull(store.busData<TestState,Float>())
    }


}