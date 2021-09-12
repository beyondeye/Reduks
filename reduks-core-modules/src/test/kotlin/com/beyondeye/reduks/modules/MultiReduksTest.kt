package com.beyondeye.reduks.modules

import org.assertj.core.api.Assertions.assertThat
import com.beyondeye.reduks.*
import org.junit.Before
import org.junit.Test

/**
 * tests for MultiReduks
 * Created by daely on 8/1/2016.
 */
class MultiReduksTest {
    @Before
    fun setUp() {
        nStateChanges1 = 0
        nStateChanges2 = 0
        nStateChanges3 = 0
        nStateChanges4 = 0
        nStateChangeCalls1 = 0
        nStateChangeCalls2 = 0
        nStateChangeCalls3 = 0
        nStateChangeCalls4 = 0
    }

    data class TestState1(val lastActionType: String = "")
    data class TestState2(val lastActionType: String = "")
    data class TestState3(val lastActionType: String = "")
    data class TestState4(val lastActionType: String = "")
    data class TestAction1(val type: String)
    data class TestAction2(val type: String)
    data class TestAction3(val type: String)
    data class TestAction4(val type: String)

    val reducer1 = ReducerFn<TestState1> { state, action ->
        when (action) {
            is TestAction1 -> state.copy(lastActionType = action.type)
            else -> state
        }
    }
    val reducer2 = ReducerFn<TestState2> { state, action ->
        when (action) {
            is TestAction2 -> state.copy(lastActionType = action.type)
            else -> state
        }
    }
    val reducer3 = ReducerFn<TestState3> { state, action ->
        when (action) {
            is TestAction3 -> state.copy(lastActionType = action.type)
            else -> state
        }
    }
    val reducer4 = ReducerFn<TestState4> { state, action ->
        when (action) {
            is TestAction4 -> state.copy(lastActionType = action.type)
            else -> state
        }
    }


    val sub1 =  StoreSubscriberBuilderFn<TestState1> { store ->
        val selector = SelectorBuilder<TestState1>()
        val selForLastAction = selector.withSingleField { this.lastActionType }
        StoreSubscriberFn {
            ++nStateChangeCalls1
            selForLastAction.onChangeIn(store.state) {
                ++nStateChanges1
            }
        }
    }
    val sub2 = StoreSubscriberBuilderFn<TestState2> { store ->
        val selector = SelectorBuilder<TestState2>()
        val selForLastAction = selector.withSingleField { this.lastActionType }
        StoreSubscriberFn {
            ++nStateChangeCalls2
            selForLastAction.onChangeIn(store.state) {
                ++nStateChanges2
            }
        }
    }
    var nStateChangeCalls3=0
    var nStateChangeCalls4=0
    var nStateChanges3=0
    var nStateChanges4=0
    val ctx3 = ReduksContext("m3")
    val ctx4 = ReduksContext("m4")
    val sub3 = StoreSubscriberBuilderFn<TestState3> { store ->
        val selector = SelectorBuilder<TestState3>()
        val selForLastAction = selector.withSingleField { this.lastActionType }
        StoreSubscriberFn {
            ++nStateChangeCalls3
            selForLastAction.onChangeIn(store.state) {
                ++nStateChanges3
            }
        }
    }
    val sub4 = StoreSubscriberBuilderFn<TestState4> { store ->
        val selector = SelectorBuilder<TestState4>()
        val selForLastAction = selector.withSingleField { this.lastActionType }
        StoreSubscriberFn {
            ++nStateChangeCalls4
            selForLastAction.onChangeIn(store.state) {
                ++nStateChanges4
            }
        }
    }

    var nStateChanges1 = 0
    var nStateChangeCalls1 = 0
    val ctx1 = ReduksContext("m1")
    //Todo: now that default module context is deprecated this test should be rewritted
    val ctx1Default= ReduksContext("ts1") //ReduksContext.default<TestState1>()
    val ctx2 = ReduksContext("m2")
    //Todo: now that default module context is deprecated this test should be rewritted
    val ctx2Default= ReduksContext("ts2") //ReduksContext.default<TestState2>()
    val mdef1 = ReduksModule.Def<TestState1>(
            ctx = ctx1,
            storeCreator = SimpleStore.Creator(),
            initialState = TestState1(),
            startAction = TestAction1("start1"),
            stateReducer = reducer1,
            subscriberBuilder = sub1
    )
    val mdef1DefaultCtx = ModuleDef<TestState1>(
            ctx1Default,
            storeCreator = SimpleStore.Creator(),
            initialState = TestState1(),
            startAction = TestAction1("start1"),
            stateReducer = reducer1,
            subscriberBuilder = sub1
    )
    var nStateChanges2 = 0
    var nStateChangeCalls2 = 0
    val mdef2 = ReduksModule.Def<TestState2>(
            ctx = ctx2,
            storeCreator = SimpleStore.Creator(),
            initialState = TestState2(),
            startAction = TestAction2("start2"),
            stateReducer = reducer2,
            subscriberBuilder = sub2
    )
    val mdef2DefaultCtx = ModuleDef<TestState2>(
            ctx=ctx2Default,
            storeCreator = SimpleStore.Creator(),
            initialState = TestState2(),
            startAction = TestAction2("start2"),
            stateReducer = reducer2,
            subscriberBuilder = sub2
    )
    val mdef3 = ReduksModule.Def<TestState3>(
            ctx = ctx3,
            storeCreator = SimpleStore.Creator(),
            initialState = TestState3(),
            startAction = TestAction3("start3"),
            stateReducer = reducer3,
            subscriberBuilder = sub3
    )
    val mdef4 = ReduksModule.Def<TestState4>(
            ctx = ctx4,
            storeCreator = SimpleStore.Creator(),
            initialState = TestState4(),
            startAction = TestAction4("start4"),
            stateReducer = reducer4,
            subscriberBuilder = sub4
    )


    @Test
    fun test_multireduks2_from_multidef_dispatch() {
        val multidef=ReduksModule.MultiDef(mdef1,mdef2)
        val mr = ReduksModule(multidef)
        assertThat(mr.store.state.s1.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.store.state.s2.lastActionType).isEqualTo("start2") //check that start action dispatched
//        assertThat(nStateChanges2).isEqualTo(1) //start action
//        assertThat(nStateChangeCalls2).isEqualTo(1) //start action
        //-----AND WHEN
        nStateChanges1=0
        nStateChangeCalls1=0
        nStateChanges2=0
        nStateChangeCalls2=0
        mr.dispatch(ActionWithContext(TestAction2("2"), ctx2))
        //-----THEN
        assertThat(mr.store.state.s1.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.store.state.s2.lastActionType).isEqualTo("2")
        assertThat(nStateChanges1).isEqualTo(0)
        assertThat(nStateChangeCalls1).isEqualTo(0)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        //-----AND WHEN
        mr.dispatch(ActionWithContext(TestAction1("1"), ctx1))
        //------THEN
        assertThat(mr.store.state.s1.lastActionType).isEqualTo("1") //check that start action dispatched
        assertThat(mr.store.state.s2.lastActionType).isEqualTo("2")

        assertThat(nStateChangeCalls1).isEqualTo(1) //start action and one additional dispatch
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)

        //-----AND WHEN
        mr.dispatch(ActionWithContext("unknown action", ctx2))
        //-----THEN
        assertThat(mr.store.state.s1.lastActionType).isEqualTo("1")
        assertThat(mr.store.state.s2.lastActionType).isEqualTo("2")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(2)
    }

    @Test
    //TEST for multidef based on default ReduksContext definitions
    fun test_multireduks2_from_multidefNoCtx_dispatch() {
        val multidef=ReduksModule.MultiDef(mdef1DefaultCtx, mdef2DefaultCtx)
        val mr = ReduksModule(multidef)
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("start2") //check that start action dispatched
//        assertThat(nStateChanges2).isEqualTo(1) //start action
//        assertThat(nStateChangeCalls2).isEqualTo(1) //start action
        //-----AND WHEN
        nStateChanges1=0
        nStateChangeCalls1=0
        nStateChanges2=0
        nStateChangeCalls2=0
        mr.dispatch(ctx2Default/TestAction2("2"))
        //-----THEN
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("2")
        assertThat(nStateChanges1).isEqualTo(0)
        assertThat(nStateChangeCalls1).isEqualTo(0)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        //-----AND WHEN
        mr.dispatch(ctx1Default/TestAction1("1"))
        //------THEN
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("2")

        assertThat(nStateChangeCalls1).isEqualTo(1) //start action and one additional dispatch
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)

        //-----AND WHEN
        mr.dispatch(ctx2Default/"unknown action")
        //-----THEN
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("1")
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("2")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(2)
    }


    @Test
     //TEST for multidef based on default ReduksContext definitions but using cached default context values
    fun test_multireduks2_from_multidefNoCtxNoReflection_dispatch() {
        val multidef=ReduksModule.MultiDef(mdef1DefaultCtx, mdef2DefaultCtx)
        val mr = ReduksModule(multidef)
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("start2") //check that start action dispatched
//        assertThat(nStateChanges2).isEqualTo(1) //start action
//        assertThat(nStateChangeCalls2).isEqualTo(1) //start action
        //-----AND WHEN
        nStateChanges1=0
        nStateChangeCalls1=0
        nStateChanges2=0
        nStateChangeCalls2=0
        mr.dispatch(ctx2Default/TestAction2("2"))
        //-----THEN
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("2")
        assertThat(nStateChanges1).isEqualTo(0)
        assertThat(nStateChangeCalls1).isEqualTo(0)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        //-----AND WHEN
        mr.dispatch(ctx1Default/TestAction1("1"))
        //------THEN
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("2")

        assertThat(nStateChangeCalls1).isEqualTo(1) //start action and one additional dispatch
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)

        //-----AND WHEN
        mr.dispatch(ctx2Default/"unknown action")
        //-----THEN
        assertThat(mr.subState<TestState1>(ctx1Default)!!.lastActionType).isEqualTo("1")
        assertThat(mr.subState<TestState2>(ctx2Default)!!.lastActionType).isEqualTo("2")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(2)
    }

//    @Ignore
    @Test
    //TEST for multidef based on two level hierarchy of modules:
    fun test_multi_level() {
        val m12 = ReduksModule.MultiDef(mdef1,mdef2)
        val m34 = ReduksModule.MultiDef(mdef3,mdef4)
        val multidef=ReduksModule.MultiDef(m12, m34)
        //check start action
//        val a:MultiActionWithContext= multidef.startAction as MultiActionWithContext
//        print(a.actionList?.size ?:0)
//        val alist=a.actionList
//        alist.forEach {
//            print(it!!.action)
//            print(it.context)
//        }
        val mr = ReduksModule(multidef)
        val ctx12_1= m12.ctx / ctx1
        val ctx12_2 = m12.ctx /ctx2
        val ctx34_3 = m34.ctx /ctx3
        val ctx34_4 = m34.ctx /ctx4

        assertThat(mr.subState<TestState1>(ctx12_1)!!.lastActionType).isEqualTo("start1") //check that start action dispatched
        assertThat(mr.subState<TestState2>(ctx12_2)!!.lastActionType).isEqualTo("start2") //check that start action dispatched
        assertThat(mr.subState<TestState3>(ctx34_3)!!.lastActionType).isEqualTo("start3") //check that start action dispatched
        assertThat(mr.subState<TestState4>(ctx34_4)!!.lastActionType).isEqualTo("start4") //check that start action dispatched

        //-----AND WHEN
        nStateChanges1=0
        nStateChanges2=0
        nStateChanges3=0
        nStateChanges4=0
        nStateChangeCalls1=0
        nStateChangeCalls2=0
        nStateChangeCalls3=0
        nStateChangeCalls4=0
        mr.dispatch(ctx12_2/TestAction2("2"))
        //-----THEN
        assertThat(mr.subState<TestState1>(ctx12_1)!!.lastActionType).isEqualTo("start1")
        assertThat(mr.subState<TestState2>(ctx12_2)!!.lastActionType).isEqualTo("2")
        assertThat(mr.subState<TestState3>(ctx34_3)!!.lastActionType).isEqualTo("start3")
        assertThat(mr.subState<TestState4>(ctx34_4)!!.lastActionType).isEqualTo("start4")
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        //-----AND WHEN
        mr.dispatch(ctx12_1/TestAction1("1"))
        //------THEN
        assertThat(mr.subState<TestState1>(ctx12_1)!!.lastActionType).isEqualTo("1")
        assertThat(mr.subState<TestState2>(ctx12_2)!!.lastActionType).isEqualTo("2")
        assertThat(mr.subState<TestState3>(ctx34_3)!!.lastActionType).isEqualTo("start3")
        assertThat(mr.subState<TestState4>(ctx34_4)!!.lastActionType).isEqualTo("start4")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        //-----AND WHEN
        mr.dispatch(ctx34_3/TestAction3("3"))
        //------THEN
        assertThat(mr.subState<TestState1>(ctx12_1)!!.lastActionType).isEqualTo("1")
        assertThat(mr.subState<TestState2>(ctx12_2)!!.lastActionType).isEqualTo("2")
        assertThat(mr.subState<TestState3>(ctx34_3)!!.lastActionType).isEqualTo("3")
        assertThat(mr.subState<TestState4>(ctx34_4)!!.lastActionType).isEqualTo("start4")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChanges3).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        assertThat(nStateChangeCalls3).isEqualTo(1)
        //-----AND WHEN
        mr.dispatch(ctx34_4/TestAction4("4"))
        //------THEN
        assertThat(mr.subState<TestState1>(ctx12_1)!!.lastActionType).isEqualTo("1")
        assertThat(mr.subState<TestState2>(ctx12_2)!!.lastActionType).isEqualTo("2")
        assertThat(mr.subState<TestState3>(ctx34_3)!!.lastActionType).isEqualTo("3")
        assertThat(mr.subState<TestState4>(ctx34_4)!!.lastActionType).isEqualTo("4")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChanges3).isEqualTo(1)
        assertThat(nStateChanges4).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(1)
        assertThat(nStateChangeCalls2).isEqualTo(1)
        assertThat(nStateChangeCalls3).isEqualTo(1)
        assertThat(nStateChangeCalls4).isEqualTo(1)


        //-----AND WHEN
        mr.dispatch(ctx12_1/"unknonw action")
        mr.dispatch(ctx12_2/"unknonw action")
        mr.dispatch(ctx34_3/"unknonw action")
        mr.dispatch(ctx34_4/"unknonw action")
        //-----THEN
        assertThat(mr.subState<TestState1>(ctx12_1)!!.lastActionType).isEqualTo("1")
        assertThat(mr.subState<TestState2>(ctx12_2)!!.lastActionType).isEqualTo("2")
        assertThat(mr.subState<TestState3>(ctx34_3)!!.lastActionType).isEqualTo("3")
        assertThat(mr.subState<TestState4>(ctx34_4)!!.lastActionType).isEqualTo("4")
        assertThat(nStateChanges1).isEqualTo(1)
        assertThat(nStateChanges2).isEqualTo(1)
        assertThat(nStateChanges3).isEqualTo(1)
        assertThat(nStateChanges4).isEqualTo(1)
        assertThat(nStateChangeCalls1).isEqualTo(2)
        assertThat(nStateChangeCalls2).isEqualTo(2)
        assertThat(nStateChangeCalls3).isEqualTo(2)
        assertThat(nStateChangeCalls4).isEqualTo(2)
    }

}