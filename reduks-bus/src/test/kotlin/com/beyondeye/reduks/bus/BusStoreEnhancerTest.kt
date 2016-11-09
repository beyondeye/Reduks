package com.beyondeye.reduks.bus

import com.beyondeye.reduks.*
import com.beyondeye.reduks.modules.*
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
val testReducerNBD = ReducerFn<TestStateWithoutBusData> { state, action ->
    when (action) {
        is Action.SetA -> state.copy(a= action.newA)
        is Action.SetB -> state.copy(b= action.newB)
        else -> state
    }
}
data class TestState(val a:Int, val b:Int, override val busData:PMap<String,Any> = emptyBusData()): StateWithBusData {
    override fun copyWithNewBusData(newBusData: PMap<String, Any>) = copy(busData=newBusData)
}
data class TestStateWithoutBusData(val a:Int,val b:Int)

val initialTestState=TestState(0,0)
val initialTestStateNBD=TestStateWithoutBusData(0,0)

class BusStoreEnhancerTest {
    @Test
    fun testBusStore() {
        //---given
        val creator= SimpleStore.Creator<TestState>()
        val store = creator.create(testReducer, initialTestState,BusStoreEnhancer())
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
        store.clearBusData<Int>()
        //---then
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<Int>())
        //---when
        store.clearBusData<Float>()
        //---then
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<Float>())

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

    @Test
    fun testBusStoreForMultiStore() {
        //---given
        val mdef1= ModuleDef(
                storeCreator = SimpleStore.Creator<TestState>().enhancedWith(BusStoreEnhancer()),
                initialState = initialTestState,
                stateReducer = testReducer
        )
        val mdef2= ModuleDef(
                storeCreator = SimpleStore.Creator<TestStateWithoutBusData>(),
                initialState = initialTestStateNBD,
                stateReducer = testReducerNBD
        )
        val multidef=ReduksModule.MultiDef(mdef1, mdef2)
        val mr = ReduksModule(multidef)
        var iDataReceivedCount:Int=0
        var fDataReceivedCount:Int=0
        //---given
        val store = mr.store as? MultiStore
        assertNotNull("the created store should be a multistore",store)
        store!!.addBusDataHandler { intVal:Int? ->
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
        store.clearBusData<Int>()
        //---then
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<Int>())
        //---when
        store.clearBusData<Float>()
        //---then
        assert(iDataReceivedCount==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<Float>())

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