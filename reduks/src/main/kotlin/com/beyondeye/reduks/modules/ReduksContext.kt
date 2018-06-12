package com.beyondeye.reduks.modules

import java.io.Serializable

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
open class ReduksContext(val moduleId:String,val modulePath:List<String>?=null):Serializable {
    /**
     * check if the context is valid
     */
    fun isValid()=moduleId.length>0
    fun hasEmptyPath()=modulePath==null || modulePath.isEmpty()
    /**
     * compose two contexts for define context for MultiStore, where the global context is defined as name of the module parts
     * TODO remove this operator, it used only in internal code, define it as an internal function
     */
    fun joinedWith(other: ReduksContext) = ReduksContext("$moduleId+${other.moduleId}",modulePath)

    /**
     * combine this context with moduleId and path of another context
     */
    infix operator fun div(other: ReduksContext): ReduksContext {
        val newPath = (modulePath ?: listOf()) +
                this.moduleId +
                (other.modulePath ?: listOf())
        return ReduksContext(other.moduleId, newPath)
    }
    /**
     * generate action with context with '/' operator
     * E.g. ctx/Action()
     * If the context is not valid then return the input action
     */
    infix operator fun div(action:Any):Any = if(isValid()) ActionWithContext(action,this) else action

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as ReduksContext

        if (moduleId != other.moduleId) return false
        if (hasEmptyPath()) {
            return if (other.hasEmptyPath()) true else false
        }

        if (modulePath!!.size != other.modulePath!!.size) return false
        var n = modulePath.size
        val it = modulePath.iterator()
        val oit = other.modulePath.iterator()
        while (--n >= 0) {
            if (it.next() != oit.next()) return false
        }
        return true
    }


    override fun toString(): String {
        val res=StringBuilder()
        modulePath?.forEach {
            res.append(it)
            res.append("/")
        }
        res.append(moduleId)
        return res.toString()
    }

    override fun hashCode(): Int {
        var result = moduleId.hashCode()
        modulePath?.forEach {
            result=result*13+it.hashCode()
        }
        return result
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