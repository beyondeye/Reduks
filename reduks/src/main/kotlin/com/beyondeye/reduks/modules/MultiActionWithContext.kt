package com.beyondeye.reduks.modules

/**
 * don't expose this class! it is used only for implementation of MultiModuleDef
 * Created by daely on 8/3/2016.
 */
internal class MultiActionWithContext(vararg a:ActionWithContext) {
    val actionList:List<ActionWithContext> = listOf(*a)
    companion object {
        internal fun toActionList(a: Any): List<Any> {
            val actionList: List<Any> =
                    if (a is MultiActionWithContext)
                        a.actionList
                    else
                        listOf(a)
            return actionList
        }
    }
}