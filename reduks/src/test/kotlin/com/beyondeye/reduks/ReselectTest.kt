package com.beyondeye.reduks

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

/**
 * Created by Dario on 3/18/2016.
 * code ported from https://github.com/reactjs/reselect/blob/master/test/test_selector.js
 * and expanded
 * TODO: support
 */


class ReselectTest {
    data class StateA(val a: Int)

    @Test
    fun basicSelectorTest() {
        val selector = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(0)
        assertThat(selector(state)).isEqualTo(0)
        assertThat(selector(state)).isEqualTo(0)
        assertThat(selector.recomputations).isEqualTo(1)
        assertThat(selector(state.copy(a = 1))).isEqualTo(1)
        assertThat(selector.recomputations).isEqualTo(2)
    }
    @Test
    fun signalChangedTest() {
        val selector = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(0)
        assertThat(selector(state)).isEqualTo(0)
        assertThat(selector(state)).isEqualTo(0)
        assertThat(selector.recomputations).isEqualTo(1)
        selector.signalChanged()
        assertThat(selector.recomputations).isEqualTo(2)
    }
    data class StateAB(val a: Int, val b: Int)
    data class StateABFloat(val a: Float, val b: Float)

    @Test
    fun basicSelectorWithMultipleKeysTest() {
        val selector = SelectorBuilder<StateAB>()
                .withField { a }
                .withField { b }
                .compute { a: Int, b: Int -> a + b }
        val state1 = StateAB(a = 1, b = 2)
        assertThat(selector(state1)).isEqualTo(3)
        assertThat(selector(state1)).isEqualTo(3)
        assertThat(selector.recomputations).isEqualTo(1)
        val state2 = StateAB(a = 3, b = 2)
        assertThat(selector(state2)).isEqualTo(5)
        assertThat(selector(state2)).isEqualTo(5)
        assertThat(selector.recomputations).isEqualTo(2)
    }
    data class StateABCFloat(val a:Float,val b:Float,val c:Float)
    @Test
    fun basicSelectorWithMultipleKeysByValueTest() {
        val selector = SelectorBuilder<StateABCFloat>()
                .withField { a }
                .withField { b }
                .compute { aa: Float, bb: Float -> aa + bb }
        val selectorByValue = SelectorBuilder<StateABCFloat>()
                .withFieldByValue { a }
                .withFieldByValue { b }
                .compute { aa: Float, bb: Float -> aa + bb }
        val state1 = StateABCFloat(a = 2.0f, b = 3.0f,c=0.0f)
        assertThat(selector(state1)).isEqualTo(5f)
        assertThat(selectorByValue(state1)).isEqualTo(5f)
        assertThat(selector.recomputations).isEqualTo(1)
        assertThat(selectorByValue.recomputations).isEqualTo(1)
        //a state with equal a,b fields by value
        val state2 = state1.copy(c=-1.0f)
        assertThat(selectorByValue(state2)).isEqualTo(5f)
        assertThat(selector(state2)).isEqualTo(5f)
        //regular selector (with argument by compared by reference) is recomputed, because the state is a different object
        assertThat(selector.recomputations).isEqualTo(2)
        //selector with arguments compared by value IS NOT RECOMPUTED, because input arguments, when compared by value are the same
        assertThat(selectorByValue.recomputations).isEqualTo(1)
    }

    data class StateSubStateA(val sub: StateA)

    @Test
    fun memoizedCompositeArgumentsTest() {
        val selector = SelectorBuilder<StateSubStateA>()
                .withField { sub }
                .compute { sub: StateA -> sub }
        val state1 = StateSubStateA(StateA(1))
        assertThat(selector(state1)).isEqualTo(StateA(1))
        assertThat(selector(state1)).isEqualTo(StateA(1))
        assertThat(selector.recomputations).isEqualTo(1)
        val state2 = StateSubStateA(StateA(2))
        assertThat(selector(state2)).isEqualTo(StateA(2))
        assertThat(selector.recomputations).isEqualTo(2)
    }


    @Test
    fun chainedSelectorTest() {
        val selector1 = SelectorBuilder<StateSubStateA>()
                .withField { sub }
                .compute { sub: StateA -> sub }
        val selector2 = SelectorBuilder<StateSubStateA>()
                .withSelector(selector1)
                .compute { sub: StateA -> sub.a }
        val state1 = StateSubStateA(StateA(1))
        assertThat(selector2(state1)).isEqualTo(1)
        assertThat(selector2(state1)).isEqualTo(1)
        assertThat(selector2.recomputations).isEqualTo(1)
        val state2 = StateSubStateA(StateA(2))
        assertThat(selector2(state2)).isEqualTo(2)
        assertThat(selector2.recomputations).isEqualTo(2)
    }


    @Test
    fun recomputationsCountTest() {
        val selector = SelectorBuilder<StateA>()
                .withField { a }
                .compute { a: Int -> a }

        val state1 = StateA(a = 1)
        assertThat(selector(state1)).isEqualTo(1)
        assertThat(selector(state1)).isEqualTo(1)
        assertThat(selector.recomputations).isEqualTo(1)
        val state2 = StateA(a = 2)
        assertThat(selector(state2)).isEqualTo(2)
        assertThat(selector.recomputations).isEqualTo(2)

        assertThat(selector(state1)).isEqualTo(1)
        assertThat(selector(state1)).isEqualTo(1)
        assertThat(selector.recomputations).isEqualTo(3)
        assertThat(selector(state2)).isEqualTo(2)
        assertThat(selector.recomputations).isEqualTo(4)
    }
    @Test
    fun primitiveFieldTest() {
        val selA = SelectorBuilder<StateABFloat>()
                .withSingleField {  a }
        val selAByValue = SelectorBuilder<StateABFloat>()
                .withSingleFieldByValue {  a }

        val state1 = StateABFloat(a = 1.0f,b=11.0f)

        val a1=selA(state1)
        assertThat(a1).isEqualTo(1.0f)
        assertThat(selA.recomputations).isEqualTo(1)
        //-----
        val a1_v=selAByValue(state1)
        assertThat(a1_v).isEqualTo(1.0f)
        assertThat(selAByValue.recomputations).isEqualTo(1)

        val state2= state1.copy(b=22.0f)
        val a2=selA(state2)
        //although we did not change a, (we changed b),
        //we recomputed because default memoization is by reference, not by value
        assertThat(a2).isEqualTo(1.0f)
        assertThat(selA.recomputations).isEqualTo(2)
        //----------------------------
        //now use memoization by value
        val a2_b=selAByValue(state2)
        assertThat(a2_b).isEqualTo(1.0f)
        //NO recomputations!!!
        assertThat(selAByValue.recomputations).isEqualTo(1)
    }
    @Test
    fun isChangedTest() {
        val selector = SelectorBuilder<StateA>()
                .withField { a }
                .compute { a: Int -> a }
        val state1 = StateA(a = 1)
        assertThat(selector(state1)).isEqualTo(1)
        assertThat(selector.isChanged()).isTrue()
        selector.resetChanged()
        assertThat(selector(state1)).isEqualTo(1)
        assertThat(selector.isChanged()).isFalse()
        val state2 = StateA(a = 2)
        assertThat(selector(state2)).isEqualTo(2)
        assertThat(selector.isChanged()).isTrue()
    }

    data class State3(val p1: Double, val p2: Double, val p3: Double)

    @Test
    fun args3Test() {
        val selector = SelectorBuilder<State3>()
                .withField { p1 }
                .withField { p2 }
                .withField { p3 }
                .compute { p1: Double, p2: Double, p3: Double -> p1 / p2 / p3 }
        val state = State3(1.0, 2.0, 3.0)
        assertThat(selector(state)).isEqualTo(1.0 / 2.0 / 3.0)
    }

    data class State4(val p1: Double, val p2: Double, val p3: Double, val p4: Double)

    @Test
    fun args4Test() {
        val selector = SelectorBuilder<State4>()
                .withField { p1 }
                .withField { p2 }
                .withField { p3 }
                .withField { p4 }
                .compute { p1: Double, p2: Double, p3: Double, p4: Double -> p1 / p2 / p3 / p4 }
        val state = State4(1.0, 2.0, 3.0, 4.0)
        assertThat(selector(state)).isEqualTo(1.0 / 2.0 / 3.0 / 4.0)
    }

    data class State5(val p1: Double, val p2: Double, val p3: Double, val p4: Double, val p5: Double)

    @Test
    fun args5Test() {
        val selector = SelectorBuilder<State5>()
                .withField { p1 }
                .withField { p2 }
                .withField { p3 }
                .withField { p4 }
                .withField { p5 }
                .compute { p1: Double, p2: Double, p3: Double, p4: Double, p5: Double -> p1 / p2 / p3 / p4 / p5 }

        val state = State5(1.0, 2.0, 3.0, 4.0, 5.0)
        assertThat(selector(state)).isEqualTo(1.0 / 2.0 / 3.0 / 4.0 / 5.0)
    }

    @Test
    fun singleFieldSelectorTest() {
        val sel4state = SelectorBuilder<State3>()
        val selp1 = sel4state.withSingleField { p1 }
        val selp2 = sel4state.withSingleField { p2 }
        val selp3 = sel4state.withSingleField { p3 }

        val state = State3(1.0, 2.0, 3.0)
        assertThat(selp1(state)).isEqualTo(1.0)
        assertThat(selp2(state)).isEqualTo(2.0)
        assertThat(selp3(state)).isEqualTo(3.0)
    }

    /*
    //test for short syntax for single field selector disabled because of kotlin compiler bug
    @Test
    fun singleFieldSelectorShortSyntaxText() {
        val sel4state = SelectorFor<State3>()
        val selp1 = sel4state{ p1 }
        val selp2 = sel4state{ p2 }
        val selp3 = sel4state{ p3 }

        val state = State3(1.0, 2.0, 3.0)
        assertThat(selp1(state)).isEqualTo(1.0)
        assertThat(selp2(state)).isEqualTo(2.0)
        assertThat(selp3(state)).isEqualTo(3.0)
    }
    */
    @Test
    fun onChangeTest() {
        val sel_a = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(a = 0)
        assertThat(sel_a(state)).isEqualTo(0)
        val changedState = state.copy(a = 1)
        var firstChangedA: Int? = null
        sel_a.onChangeIn(changedState) {
            firstChangedA = it
        }
        var secondChangedA: Int? = null
        sel_a.onChangeIn(changedState) {
            secondChangedA = it
        }
        assertThat(firstChangedA).isEqualTo(1)
        assertThat(secondChangedA).isNull()

    }
    @Test
    fun onChangeConditionalTest() {
        val sel_a = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(a = 0)
        assertThat(sel_a(state)).isEqualTo(0)
        val changedState = state.copy(a = 1)
        var firstChangedA: Int? = null
        //this first time the selector is not run, because condition is false
        sel_a.onChangeIn(changedState,false) {
            firstChangedA = it
        }
        var secondChangedA: Int? = null
        //this second time the selector is run, because condition is true
        sel_a.onChangeIn(changedState,true) {
            secondChangedA = it
        }
        assertThat(firstChangedA).isNull()
        assertThat(secondChangedA).isEqualTo(1)
    }
    @Test
    fun onChangedWithMemoizedComputeTest() {
        val sel_sum = SelectorBuilder<State3>()
                .withField { p1 }
                .withField { p2 }.compute { p1, p2 ->p1+p2  }
        val sel_sum_memoized = SelectorBuilder<State3>()
                .withField { p1 }
                .withField { p2 }.compute { p1, p2 ->p1+p2  }
                .computeResultMemoizedByVal()


        val state= State3(1.0,2.0,3.0)
        assertThat(sel_sum(state)).isEqualTo(3.0)
        assertThat(sel_sum_memoized(state)).isEqualTo(3.0)


        val changedState= State3(2.0,1.0,3.0) //switched order of p1,p2 but same sum
        var sel_sum_changed_count: Int = 0
        sel_sum.onChangeIn(changedState) {value:Double -> sel_sum_changed_count+=1 }
        sel_sum.onChangeIn(changedState) {value:Double ->
            //this selector is run, because inputs are changed (changedState)
            sel_sum_changed_count+=1
        }
        var sel_sum_memoized_changed_count: Int = 0
        sel_sum_memoized.onChangeIn(changedState) {value:Double -> sel_sum_memoized_changed_count+=1 }
        sel_sum_memoized.onChangeIn(changedState) {value:Double ->
            //this selector is not run, because although inputs are changed (changedState) compute result is not changed
            sel_sum_memoized_changed_count+=1
        }
        assertThat(sel_sum_changed_count).isEqualTo(2)
        assertThat(sel_sum_memoized_changed_count).isEqualTo(1)
    }


    @Test
    fun whenChangedTest() {
        val sel_a = SelectorBuilder<StateA>().withSingleField { a }
        val state = StateA(a = 0)
        assertThat(sel_a(state)).isEqualTo(0)
        val changedState = state.copy(a = 1)
        var firstChangedA: Int? = null
        var secondChangedA: Int? = null
        with(changedState) {
            whenChangeOf(sel_a) {
                firstChangedA = it
            }
            whenChangeOf(sel_a) {
                secondChangedA = it
            }
            assertThat(firstChangedA).isEqualTo(1)
            assertThat(secondChangedA).isNull()

        }
    }
}

