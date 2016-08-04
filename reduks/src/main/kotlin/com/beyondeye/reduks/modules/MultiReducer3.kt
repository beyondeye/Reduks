package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer

interface MultiReducer3<S1:Any,S2:Any,S3:Any> : Reducer<MultiState3<S1, S2, S3>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
}