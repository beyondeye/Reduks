package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer

interface MultiReducer6<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any,S6:Any> : Reducer<MultiState6<S1,S2,S3,S4,S5,S6>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
    val r4: Reducer<S4>
    val r5: Reducer<S5>
    val r6: Reducer<S6>
}