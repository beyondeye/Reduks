package com.beyondeye.reduks.modules

/**
 *use @JvmField annotation for avoiding generation useless getter methods
 * Created by daely on 8/4/2016.
 */
class MultiState2<S1:Any,S2:Any>(@JvmField val ctx:ReduksContext,
                                 @JvmField val s1:S1,
                                 @JvmField val s2:S2) {
    fun copy(s1: S1?=null, s2:S2?=null) =
            MultiState2(ctx,s1?:this.s1,s2?:this.s2)
}
class MultiState3<S1:Any,S2:Any,S3:Any>(@JvmField val ctx:ReduksContext,
                                        @JvmField val s1:S1,
                                        @JvmField val s2:S2,
                                        @JvmField val s3:S3) {
    fun copy(s1: S1?=null, s2:S2?=null, s3:S3?=null) =
            MultiState3(ctx,s1?:this.s1,s2?:this.s2,s3?:this.s3)
}
class MultiState4<S1:Any,S2:Any,S3:Any,S4:Any>(@JvmField val ctx:ReduksContext,
                                               @JvmField val s1:S1,
                                               @JvmField val s2:S2,
                                               @JvmField val s3:S3,
                                               @JvmField val s4:S4){
    fun copy(s1: S1?=null, s2:S2?=null, s3:S3?=null, s4:S4?=null) =
            MultiState4(ctx,s1?:this.s1,s2?:this.s2,s3?:this.s3,s4?:this.s4)
}
class MultiState5<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any>(@JvmField val ctx:ReduksContext,
                                                      @JvmField val s1:S1,
                                                      @JvmField val s2:S2,
                                                      @JvmField val s3:S3,
                                                      @JvmField val s4:S4,
                                                      @JvmField val s5:S5) {
    fun copy(s1: S1?=null, s2:S2?=null, s3:S3?=null, s4:S4?=null, s5:S5?=null) =
            MultiState5(ctx,s1?:this.s1,s2?:this.s2,s3?:this.s3,s4?:this.s4,s5?:this.s5)
}
class MultiState6<S1:Any,S2:Any,S3:Any,S4:Any,S5:Any,S6:Any>(@JvmField val ctx:ReduksContext,
                                                             @JvmField val s1:S1,
                                                             @JvmField val s2:S2,
                                                             @JvmField val s3:S3,
                                                             @JvmField val s4:S4,
                                                             @JvmField val s5:S5,
                                                             @JvmField val s6:S6) {
    fun copy(s1: S1?=null, s2:S2?=null, s3:S3?=null, s4:S4?=null, s5:S5?=null, s6:S6?=null) =
            MultiState6(ctx,s1?:this.s1,s2?:this.s2,s3?:this.s3,s4?:this.s4,s5?:this.s5,s6?:this.s6)
}