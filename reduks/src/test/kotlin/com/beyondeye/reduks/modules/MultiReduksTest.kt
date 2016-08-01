package com.beyondeye.reduks.modules

import com.beyondeye.reduks.*
import org.junit.Assert.*

/**
 * tests for MultiReduks
 * Created by daely on 8/1/2016.
 */
class MultiReduksTest{
    data class TestState1(val lastActionType:String="")
    data class TestState2(val lastActionType:String="")
    data class TestAction1(val type: String)
    data class TestAction2(val type: String)
    val reducer1 = Reducer<TestState1> { state, action ->
        when (action) {
            is TestAction1 -> state.copy(lastActionType = action.type)
            else -> state
        }
    }
    val reducer2 = Reducer<TestState2> { state, action ->
        when (action) {
            is TestAction1 -> state.copy(lastActionType = action.type)
            else -> state
        }
    }
    val m1=object:ReduksModuleDef<TestState1>() {
        override val storeFactory= SimpleStore.Factory<TestState1>()
        override val initialState= TestState1()
        override val startAction: Any = TestAction1("1")
        override val stateReducer = reducer1
        override fun getStoreSubscriber(): (Store<TestState1>) -> StoreSubscriber<TestState1> {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
    val m2=object:ReduksModuleDef<TestState2>() {
        override val storeFactory= SimpleStore.Factory<TestState2>()
        override val initialState= TestState2()
        override val startAction: Any = TestAction2("2")
        override val stateReducer = reducer2
        override fun getStoreSubscriber(): (Store<TestState2>) -> StoreSubscriber<TestState2> {
            throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }
    val mr=MultiReduks.buildFromModules(m1,m2)

}