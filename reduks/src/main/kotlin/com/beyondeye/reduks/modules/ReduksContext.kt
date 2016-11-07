package com.beyondeye.reduks.modules

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
class ReduksContext(val moduleId:String) {
    /**
     * compose two contexts
     */
    infix operator fun plus(other: ReduksContext) = ReduksContext("$moduleId+${other.moduleId}")
    /**
     * generate action with context with '..' operator
     * E.g. ctx..Action()
     */
    operator fun rangeTo(action:Any) = ActionWithContext(action,this)
}