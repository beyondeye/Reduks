package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Reducer

/**
 * set of  separate reducers for  separate states
 * Created by daely on 8/3/2016.
 */
class MultiReducer2<S1:Any,S2:Any>(m1: ReduksModule.Def<S1>,
                                   m2: ReduksModule.Def<S2>) : Reducer<MultiState2<S1, S2>> {
    @JvmField val r1: Reducer<S1> = m1.stateReducer //use @JvmField annotation for avoiding generation useless getter methods
    @JvmField val r2: Reducer<S2> = m2.stateReducer
    @JvmField val ctx1 = m1.ctx
    @JvmField val ctx2 = m2.ctx
    override fun reduce(s: MultiState2<S1, S2>, a: Any): MultiState2<S1, S2> {
        val actionList= MultiActionWithContext.toActionList(a)
        var newS = s
        actionList.forEach {
            if (a is ActionWithContext) {
                newS = when (a.context) {
                    ctx1 -> newS.copy(s1 = r1.reduce(newS.s1, a.action))
                    ctx2 -> newS.copy(s2 = r2.reduce(newS.s2, a.action))
                    else -> newS
                }
            }
        }
        return newS
    }
}