package com.beyondeye.reduks.modules

/**
 * Created by daely on 8/4/2016.
 */
data class MultiState2<S1:Any,S2:Any>(val ctx:ReduksContext,val s1:S1,val s2:S2)
data class MultiState3<S1:Any,S2:Any,S3:Any>(val ctx:ReduksContext,val s1:S1,val s2:S2,val s3:S3)
data class MultiState4<S1:Any,S2:Any,S3:Any,S4:Any>(val ctx:ReduksContext,val s1:S1,val s2:S2,val s3:S3,val s4:S4)
data class MultiState5<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any>(val ctx:ReduksContext,val s1:S1,val s2:S2,val s3:S3,val s4:S4,val s5:S5)
data class MultiState6<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any,S6:Any>(val ctx:ReduksContext,val s1:S1,val s2:S2,val s3:S3,val s4:S4,val s5:S5,val s6:S6)