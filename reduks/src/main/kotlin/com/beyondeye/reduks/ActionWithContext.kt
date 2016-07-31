package com.beyondeye.reduks

/**
 * Action object with attached ActionContext: this wrapped action is used when combining multiple Reduks modules
 * Created by daely on 7/31/2016.
 */
data class ActionWithContext(val action:Any,val context: ActionContext)