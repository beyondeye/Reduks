package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer

/**
 * set of  separate reducers for  separate states
 * Created by daely on 8/3/2016.
 */
class MultiReducer5<S1 : Any, S2 : Any, S3 : Any, S4 : Any, S5 : Any>(
        m1: ReduksModule.Def<S1>,
        m2: ReduksModule.Def<S2>,
        m3: ReduksModule.Def<S3>,
        m4: ReduksModule.Def<S4>,
        m5: ReduksModule.Def<S5>) : Reducer<MultiState5<S1, S2, S3, S4, S5>> {
    @JvmField val r1: Reducer<S1> = m1.stateReducer  //use @JvmField annotation for avoiding generation useless getter methods
    @JvmField val r2: Reducer<S2> = m2.stateReducer
    @JvmField val r3: Reducer<S3> = m3.stateReducer
    @JvmField val r4: Reducer<S4> = m4.stateReducer
    @JvmField val r5: Reducer<S5> = m5.stateReducer
    @JvmField val ctx1 = m1.ctx
    @JvmField val ctx2 = m2.ctx
    @JvmField val ctx3 = m3.ctx
    @JvmField val ctx4 = m4.ctx
    @JvmField val ctx5 = m5.ctx
    override fun reduce(s: MultiState5<S1, S2, S3, S4, S5>, a: Any): MultiState5<S1, S2, S3, S4, S5> {
        val actionList: List<Any> = MultiActionWithContext.toActionList(a)
        var newS = s
        actionList.forEach {
            if (a is ActionWithContext) {
                newS = when (a.context) {
                    ctx1 -> newS.copy(s1 = r1.reduce(newS.s1, a.action))
                    ctx2 -> newS.copy(s2 = r2.reduce(newS.s2, a.action))
                    ctx3 -> newS.copy(s3 = r3.reduce(newS.s3, a.action))
                    ctx4 -> newS.copy(s4 = r4.reduce(newS.s4, a.action))
                    ctx5 -> newS.copy(s5 = r5.reduce(newS.s5, a.action))
                    else -> newS
                }
            }
        }
        return newS
    }
}