package com.beyondeye.reduks

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
data class ActionContext(val moduleId:String,val moduleInstanceId:String)