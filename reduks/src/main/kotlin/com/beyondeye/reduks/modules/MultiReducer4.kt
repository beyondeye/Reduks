package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer

interface MultiReducer4<S1:Any,S2:Any,S3:Any,S4:Any> : Reducer<MultiState4<S1, S2, S3, S4>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
    val r4: Reducer<S4>
}