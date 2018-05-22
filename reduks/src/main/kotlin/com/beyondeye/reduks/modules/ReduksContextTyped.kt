package com.beyondeye.reduks.modules

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
class ReduksContextTyped<ActionType:Any>(moduleId:String,modulePath:List<String>?=null): ReduksContext(moduleId,modulePath) {
    /**
     * generate action with context with '/' operator
     * E.g. ctx/Action()
     * If the context is not valid then return the input action
     * The action type must match the Action Type associated with this context
     */
  //  infix operator fun div(action:ActionType):Any = if(isValid()) ActionWithContext(action, this) else action

}