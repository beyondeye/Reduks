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
    fun testBusStore() {
        //---given
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

        //---when
        val iBusDataBeforePost:Int? =store.busData()
        //---then
        assertNull(iBusDataBeforePost)

        //---when
        val fBusDataBeforePost:Float? =store.busData()
        //---then
        assertNull(fBusDataBeforePost)

        //---when
        store.postBusData(2.0F)
        //---then
        assert(fDataReceivedCount==1)
        assert(iDataReceivedCount==0)

        //---when
        store.postBusData(1)
        //---then
        assert(iDataReceivedCount==1)
        assert(fDataReceivedCount==1)

        //---when
        val ibusData:Int?=store.busData()
        //---then
        assertNotNull(ibusData)
        assertEquals(ibusData ,1)

        //---when
        val fbusData:Float?=store.busData()
        //---then
        assertNotNull(fbusData)
        assertEquals(fbusData ,2.0f)

        //---given
        fDataReceivedCount=0
        iDataReceivedCount=0
        //---when
        //check that normal actions goes through as expected
        store.dispatch(Action.SetA(1))
        //---then
        assertTrue(store.state.a==1)
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)


        //---when
        store.clearBusData<TestState,Int>()
        //---then
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<TestState,Int>())
        //---when
        store.clearBusData<TestState,Float>()
        //---then
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<TestState,Float>())

        //---when
        store.unsubscribeAllBusDataHandlers()
        store.postBusData(11)
        //---then
        assert(fDataReceivedCount==0)
        assert(iDataReceivedCount==0)
        //---when
        store.postBusData(22.0f)
        //---then
        assert(fDataReceivedCount==0)
        assert(iDataReceivedCount==0)
    }


}