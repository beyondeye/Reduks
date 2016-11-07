package com.beyondeye.reduks.modules

import org.junit.Test

import org.junit.Assert.*

/**
 * Created by daely on 11/7/2016.
 */
class ReduksContextTest {
    @Test
    fun testThatActionComposedWithInvalidContextReturnTheAction() {
        //.....given
        val action_without_context="the action"
        val invalidActionContext=ReduksContext("")
        //.....when
        val actionWithContext=invalidActionContext..action_without_context
        //....then
        assertTrue(actionWithContext===action_without_context)
    }

}