package com.beyondeye.reduks

import com.beyondeye.reduks.pcollections.PVector
import com.beyondeye.reduks.pcollections.TreePVector
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MyTreePVectorTests {
    @Test
    fun testPVectorWithElementChanged() {
        var pvec:PVector<Int> = TreePVector.empty<Int>()
        for (i in 0 .. 9) {
            pvec= pvec.plus(i)
        }
        assertThat(pvec[4]).isEqualTo(4)

        pvec = pvec.with(4,44)

        assertThat(pvec.size).isEqualTo(10)
        assertThat(pvec[4]).isEqualTo(44)
    }
}