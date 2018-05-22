package com.beyondeye.reduks.modules

import org.junit.Assert.*
import org.junit.Test

/**
 * Created by daely on 11/7/2016.
 */
class ActionWithContextTest {
    @Test
    fun testActionWithContextLambdaMatcher() {
        class ActionA
        class ActionB
        class ActionC
        val ctx1= ReduksContext("1")
        val ctx2= ReduksContext("2")
        val ctx3= ReduksContext("3")
        fun getMatch(a:Any)= when(a) {
            ctx1.matchA { it is ActionA } -> "1a"
            ctx1.matchA { it is ActionB } -> "1b"
            ctx2.matchA { it is ActionA } -> "2a"
            ctx2.matchA { it is ActionB } -> "2b"
            else -> ""
        }
        assertEquals("1a",getMatch(ctx1/ActionA()))
        assertEquals("1b",getMatch(ctx1/ActionB()))
        assertEquals("2a",getMatch(ctx2/ActionA()))
        assertEquals("2b",getMatch(ctx2/ActionB()))
        assertEquals("",getMatch(ctx1/ActionC()))
        assertEquals("",getMatch(ctx3/ActionA()))
        assertEquals("",getMatch(ctx3/ActionB()))
    }
    @Test
    fun testActionWithContextIsAMatcher() {
        class ActionA
        class ActionB
        class ActionC
        val ctx1= ReduksContext("1")
        val ctx2= ReduksContext("2")
        val ctx3= ReduksContext("3")
        fun getMatch(a:Any)= when(a) {
            ctx1.isA<ActionA>() -> "1a"
            ctx1.isA<ActionB>() -> "1b"
            ctx2.isA<ActionA>() -> "2a"
            ctx2.isA<ActionB>() -> "2b"
            else -> ""
        }
        assertEquals("1a",getMatch(ctx1/ActionA()))
        assertEquals("1b",getMatch(ctx1/ActionB()))
        assertEquals("2a",getMatch(ctx2/ActionA()))
        assertEquals("2b",getMatch(ctx2/ActionB()))
        assertEquals("",getMatch(ctx1/ActionC()))
        assertEquals("",getMatch(ctx3/ActionA()))
        assertEquals("",getMatch(ctx3/ActionB()))
    }
}
