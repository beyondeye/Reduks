package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer

/**
 * set of  separate reducers for  separate states
 * Created by daely on 8/3/2016.
 */
interface MultiReducer2<S1:Any,S2:Any> : Reducer<MultiState2<S1,S2>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
}

interface MultiReducer3<S1:Any,S2:Any,S3:Any> : Reducer<MultiState3<S1,S2,S3>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
}

interface MultiReducer4<S1:Any,S2:Any,S3:Any,S4:Any> : Reducer<MultiState4<S1,S2,S3,S4>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
    val r4: Reducer<S4>
}

interface MultiReducer5<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any> : Reducer<MultiState5<S1,S2,S3,S4,S5>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
    val r4: Reducer<S4>
    val r5: Reducer<S5>
}

interface MultiReducer6<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any,S6:Any> : Reducer<MultiState6<S1,S2,S3,S4,S5,S6>> {
    val r1: Reducer<S1>
    val r2: Reducer<S2>
    val r3: Reducer<S3>
    val r4: Reducer<S4>
    val r5: Reducer<S5>
    val r6: Reducer<S6>
}