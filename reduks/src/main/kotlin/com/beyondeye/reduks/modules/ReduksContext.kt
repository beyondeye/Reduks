package com.beyondeye.reduks.modules

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
open class ReduksContext(val moduleId:String) {
    /**
     * check if the context is valid
     */
    fun isValid()=moduleId.length>0
    /**
     * compose two contexts
     */
    infix operator fun plus(other: ReduksContext) = ReduksContext("$moduleId+${other.moduleId}")
    /**
     * generate action with context with '..' operator
     * E.g. ctx..Action()
     * If the context is not valid then return the input action
     */
    operator fun rangeTo(action:Any):Any = if(isValid()) ActionWithContext(action,this) else action

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ReduksContext

        if (moduleId != other.moduleId) return false

        return true
    }

    override fun toString(): String {
        return moduleId
    }

    override fun hashCode(): Int {
        return moduleId.hashCode()
    }
    companion object {
        /**
         * default ReduksContext for some state type is the state class simple name
         */
        inline fun<reified S:Any> default() = ReduksContext(defaultModuleId<S>())
        /**
         * default ReduksContext for some state type is the state class simple name and
         * the action type for that context
         *
         */
        inline fun<reified S:Any,ActionType:Any> defaultTyped() =ReduksContextTyped<ActionType>(defaultModuleId<S>())

        inline fun<reified S:Any> defaultModuleId() =S::class.java.simpleName
    }
}