package com.beyondeye.reduks.modules

import org.junit.Test

import org.junit.Assert.*
import org.assertj.core.api.Assertions.assertThat

/**
 * Created by daely on 11/7/2016.
 */
class ReduksContextTest {
    @Test
    fun testThatActionComposedWithInvalidContextReturnTheAction() {
        //.....given
        val action_without_context="the action"
        val invalidActionContext= ReduksContext("")
        //.....when
        val actionWithContext=invalidActionContext/action_without_context
        //....then
        assertTrue(actionWithContext===action_without_context)
    }
    @Test
    fun testContextWithPath() {
        val a_b=ReduksContext("a")/ReduksContext("b")
        assertThat(a_b.toString()).isEqualTo("a/b")
        val a_b_explicit_def=ReduksContext("b", listOf("a"))
        assertThat(a_b).isEqualTo(a_b_explicit_def)
        val b_a_explicit_def=ReduksContext("a", listOf("b"))
        assertThat(a_b).isNotEqualTo(b_a_explicit_def)

        val a_b_c=a_b/ ReduksContext("c")
        assertThat(a_b_c.toString()).isEqualTo("a/b/c")
        assertThat(a_b_c.modulePath!!.size).isEqualTo(2)
        assertThat(a_b).isNotEqualTo(a_b_c)
        val a_b_c_2= ReduksContext("a")/ReduksContext("b")/ReduksContext("c")
        assertThat(a_b_c).isEqualTo(a_b_c_2)
        val a_b_d=a_b/ReduksContext("d")
        assertThat(a_b_d).isNotEqualTo(a_b_c)
    }
    @Test
    fun testDobleWrappingComposedAsPath() {
        val a = ActionWithContext("the action", ReduksContext("a"))
        val wrapped_a=ActionWithContext(a,ReduksContext("b"))
        assertThat(wrapped_a.context.toString()).isEqualTo("b/a")
        val wrapped_wrapped_a=ActionWithContext(wrapped_a,ReduksContext("c"))
        assertThat(wrapped_wrapped_a.context.toString()).isEqualTo("c/b/a")
    }
}