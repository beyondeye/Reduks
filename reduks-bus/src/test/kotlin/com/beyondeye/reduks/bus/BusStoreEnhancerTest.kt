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
val testReducer = ReducerFn<TestStateWithBusData> { state, action ->
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
val ctx_TestStateWithBusData=ReduksContext("ctx1")
data class TestStateWithBusData(val a:Int, val b:Int, override val busData: BusData = BusData.empty): StateWithBusData {
    override fun copyWithNewBusData(newBusData: BusData) = copy(busData=newBusData)
}

val ctx_TestStateWithoutBusData=ReduksContext("ctx2")
data class TestStateWithoutBusData(val a:Int,val b:Int)

val initialTestState= TestStateWithBusData(0,0)
val initialTestStateNBD=TestStateWithoutBusData(0,0)

val mdef1= ModuleDef(
        ctx_TestStateWithBusData,
        storeCreator = SimpleStore.Creator<TestStateWithBusData>().enhancedWith(BusStoreEnhancer()),
        initialState = initialTestState,
        stateReducer = testReducer
)
val mdef2= ModuleDef(
        ctx_TestStateWithoutBusData,
        storeCreator = SimpleStore.Creator<TestStateWithoutBusData>(),
        initialState = initialTestStateNBD,
        stateReducer = testReducerNBD
)

class BusStoreEnhancerTest {
    @Test
    fun testBusStore() {
        //---given
        val creator= SimpleStore.Creator<TestStateWithBusData>()
        val store = creator.create(testReducer, initialTestState,BusStoreEnhancer())
        var iDataReceivedCount:Int=0
        var iHandlerCalls=0
        var fDataReceivedCount:Int=0
        var fHandlerCalls=0
        val subscriptions = mutableListOf<StoreSubscription>()
        val h1=store.addBusDataHandler { intVal:Int? ->
            val receivedOnBus=intVal
            ++iHandlerCalls
            if(receivedOnBus!=null) {
                iDataReceivedCount++
                assertEquals(receivedOnBus,1)
            }
        }
        subscriptions.add(h1!!)
        val h2=store.addBusDataHandler { floatVal:Float? ->
            val receivedOnBus=floatVal
            ++fHandlerCalls
            if(receivedOnBus!=null) {
                fDataReceivedCount++
                assertEquals(receivedOnBus,2.0F)
            }
        }
        subscriptions.add(h2!!)

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
        assert(fHandlerCalls==1)
        assert(iDataReceivedCount==0)
        assert(iHandlerCalls==1) //!!!with the first post on the bus, all bus data handlers are called with null TODO: document this

        //---when
        iHandlerCalls=0
        iDataReceivedCount=0
        fHandlerCalls=0
        fDataReceivedCount=0
        store.postBusData(1)
        //---then
        assert(iHandlerCalls==1)
        assert(iDataReceivedCount==1)
        assert(fHandlerCalls==0)
        assert(fDataReceivedCount==0)

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
        fHandlerCalls=0
        fDataReceivedCount=0
        iHandlerCalls=0
        iDataReceivedCount=0
        //---when
        //check that normal actions goes through as expected
        store.dispatch(Action.SetA(1))
        //---then
        assertTrue(store.state.a==1)
        assert(iHandlerCalls==0)
        assert(iDataReceivedCount==0)
        assert(fHandlerCalls==0)
        assert(fDataReceivedCount==0)


        //---when
        fHandlerCalls=0
        iHandlerCalls=0
        store.clearBusData<Int>()
        //---then
        assert(iHandlerCalls==1) //data cleared message received
        assert(iDataReceivedCount==0)
        assert(fHandlerCalls==0)
        assert(fDataReceivedCount==0)
        assertNull(store.busData<Int>())
        //---when
        fHandlerCalls=0
        iHandlerCalls=0
        store.clearBusData<Float>()
        //---then
        assert(iHandlerCalls==0)
        assert(iDataReceivedCount==0)
        assert(fHandlerCalls==1) //data cleared message received
        assert(fDataReceivedCount==0)
        assertNull(store.busData<Float>())

        //---when
        fHandlerCalls=0
        iHandlerCalls=0
        subscriptions.forEach { it.unsubscribe() } //remove all busdata handlers
        store.postBusData(11)
        //---then
        assert(fHandlerCalls==0)
        assert(fDataReceivedCount==0)
        assert(iHandlerCalls==0)
        assert(iDataReceivedCount==0)
        //---when
        fHandlerCalls=0
        iHandlerCalls=0
        store.postBusData(22.0f)
        //---then
        assert(fHandlerCalls==0)
        assert(fDataReceivedCount==0)
        assert(iHandlerCalls==0)
        assert(iDataReceivedCount==0)
    }


    @Test
    fun testBusStoreForMultiStore() {
        //---given
        val multidef=ReduksModule.MultiDef(mdef1, mdef2)
        val mr = ReduksModule(multidef)
        var iDataReceivedCount:Int=0
        var fDataReceivedCount:Int=0
        //---given
        val store = mr.store as? MultiStore
        assertNotNull("the created store should be a multistore",store)
        val subscriptions = mutableListOf<StoreSubscription>()
        val h1=store!!.addBusDataHandler { intVal:Int? ->
            val receivedOnBus=intVal
            if(receivedOnBus!=null) {
                iDataReceivedCount++
                assertEquals(receivedOnBus,1)
            }
        }
        subscriptions.add(h1!!)
        //test Reduks.addBusDataHandler extension function
        val h2=mr.addBusDataHandler("testtag") { floatVal:Float? ->
            val receivedOnBus=floatVal
            if(receivedOnBus!=null) {
                fDataReceivedCount++
                assertEquals(receivedOnBus,2.0F)
            }
        }
        subscriptions.add(h2!!)

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
        subscriptions.forEach { it.unsubscribe() } //remove all busdata handlers
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
    class BusDataA(val payloadA:String)
    class BusDataB(val payloadB:String)
    @Test
    fun testAddRemoveBusDataHandlers() {
        //-------GIVEN
        val multidef=ReduksModule.MultiDef(mdef1, mdef2)
        val mr = ReduksModule(multidef)
        assert(mr.busStoreSubscriptionsByTag.size==0)
        //-------WHEN
        val busStore=mr.subStore<TestStateWithBusData>(ctx_TestStateWithBusData) as? BusStore
        //-------THEN
        assert(busStore!=null)
        //------AND WHEN
        mr.addBusDataHandler<BusDataA>("atag") {
            throw NotImplementedError("Dummy handler!")
        }
        //------THEN
        assert((mr.busStoreSubscriptionsByTag.size==1))
        //------AND WHEN
        mr.addBusDataHandler<BusDataB>("btag") {
            throw NotImplementedError("Dummy handler!")
        }
        //------THEN
        assert((mr.busStoreSubscriptionsByTag.size==2))
        var busSubscriptionsA=mr.busStoreSubscriptionsByTag["atag"]
        assert(busSubscriptionsA!=null && busSubscriptionsA.size==1)
        var busSubscriptionsB=mr.busStoreSubscriptionsByTag["btag"]
        assert(busSubscriptionsB!=null && busSubscriptionsB.size==1)
        //-----AND WHEN
        mr.removeAllBusDataHandlers()
        //------THEN
        busSubscriptionsA=mr.busStoreSubscriptionsByTag["atag"]
        assert(busSubscriptionsA==null || busSubscriptionsA.size==0)
        busSubscriptionsB=mr.busStoreSubscriptionsByTag["btag"]
        assert(busSubscriptionsB==null || busSubscriptionsB.size==0)

    }
    @Test
    fun testRemoveBusDataHandlerWithTag() {
        val multidef=ReduksModule.MultiDef(mdef1, mdef2)
        val mr = ReduksModule(multidef)
        //----AND GIVEN
        mr.addBusDataHandler<BusDataA>("atag") {
            throw NotImplementedError("Dummy handler!")
        }
        mr.addBusDataHandler<BusDataB>("btag") {
            throw NotImplementedError("Dummy handler!")
        }
        val busSubscriptionsA=mr.busStoreSubscriptionsByTag["atag"]
        val busSubscriptionsB=mr.busStoreSubscriptionsByTag["btag"]
        //----WHEN
        mr.removeBusDataHandlers("atag")
        //---THEN
        assert(busSubscriptionsA!!.size==0)
        assert(busSubscriptionsB!!.size==1)
        //----AND WHEN
        mr.removeBusDataHandlers("btag")
        //---THEN
        assert(busSubscriptionsA.size==0)
        assert(busSubscriptionsB.size==0)
    }

}