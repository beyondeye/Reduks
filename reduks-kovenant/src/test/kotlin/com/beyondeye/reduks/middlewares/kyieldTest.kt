package com.beyondeye.reduks.middlewares

/**
 * Created with IntelliJ IDEA.
 * User: Nerzhul
 * Date: 06.02.13
 * Time: 1:17
 * To change this template use File | Settings | File Templates.
 */

import junit.framework.TestCase
import nl.komponents.kovenant.task
import org.junit.Test
import java.util.NoSuchElementException


class KYieldTest :TestCase(){
    fun yieldEmpty() = yieldfun<Int> {
    }

    fun yieldRet1() = yieldfun<Int> {
        ret kyield 1
    }

    fun yieldRet123() = yieldfun<Int> {
        for (i in 1 .. 3) {
            ret kyield i
        }
    }
    fun yieldPromises123() = yieldfun<Int> {
        for (i in 1 .. 3) {
            ret kyield task { i }
        }
    }
    fun yieldEvens() = yieldfun<Int> {
        for (i in 0 .. 8) {
            if (i % 2 == 0)
                ret kyield i
        }
        ret kyield 10
    }

    fun yieldInfinite() : Iterable<Int> = yieldfun {
        var i = 0
        while (true) {
            ret kyield i++
        }
    }

    fun yieldThrows() = yieldfun<Int> {
        ret kyield 1
        ret kyield 2
        throw Exception("some exception")
    }
    fun yieldThrowsPromises() = yieldfun<Int> {
        ret kyield task {1}
        ret kyield task {2}
        ret kyield task { throw Exception("some exception") }
    }


    @Test fun testYieldEmpty() {
        val empty = yieldEmpty().toList()
        assertEquals(listOf<Int>(), empty)
    }

    @Test fun testYieldRet1() {
        val one = yieldRet1().toList()
        assertEquals(listOf(1), one)
    }

    @Test fun testYieldRet123() {
        val list = yieldRet123().toList()
        assertEquals(listOf(1, 2, 3), list)
    }

    @Test fun testYeldRet123Promises() {
        val list = yieldPromises123().toList()
        assertEquals(listOf(1, 2, 3), list)
    }

    @Test fun testYieldEvens() {
        val list = yieldEvens().toList()
        assertEquals(listOf(0, 2, 4, 6, 8, 10), list)
    }

    @Test fun testYieldInfiniteTake3() {
        val list = yieldInfinite().take(3)
        assertEquals(listOf(0, 1, 2), list)
    }

    @Test fun testYieldExpectExceptionInNext() {
        val empty = yieldEmpty()
        val iterator = empty.iterator()
        assertFalse(iterator.hasNext())
        fails{
            iterator.next()
        } is NoSuchElementException
    }

    class NoException:Exception()
    private fun  fails(fn2check: () -> Unit): Exception {
        var res:Exception=NoException()
        try {
            fn2check
        } catch (e:Exception) {
            res=e
        }
        return res
    }

    @Test fun testManyHasNextCalls() {
        val iterable = yieldRet1()
        val iterator = iterable.iterator()
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        assertTrue(iterator.hasNext())
        val i = iterator.next()
        assertEquals(1, i)
        assertFalse(iterator.hasNext())
    }

    @Test fun testYieldThrows() {
        val iterable = yieldThrows()
        fails {
            iterable.toList()
        } is Exception
    }
    @Test fun testYieldThrowsPromises() {
        val iterable = yieldThrowsPromises()
        fails {
            iterable.toList()
        } is Exception
    }


}