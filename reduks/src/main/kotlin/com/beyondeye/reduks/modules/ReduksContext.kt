package com.beyondeye.reduks.modules

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
class ReduksContext(val moduleId:String) {
    /**
     * check if the context is valid
     */
    fun isEmpty()=moduleId.length>0
    /**
     * compose two contexts
     */
    infix operator fun plus(other: ReduksContext) = ReduksContext("$moduleId+${other.moduleId}")
    /**
     * generate action with context with '..' operator
     * E.g. ctx..Action()
     * If the context is not valid then return the input action
     */
    operator fun rangeTo(action:Any):Any = if(isEmpty()) ActionWithContext(action,this) else action

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ReduksContext

        if (moduleId != other.moduleId) return false

        return true
    }

    override fun hashCode(): Int {
        return moduleId.hashCode()
    }

}