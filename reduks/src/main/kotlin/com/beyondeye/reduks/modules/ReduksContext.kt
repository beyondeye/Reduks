package com.beyondeye.reduks.modules

/**
 * context for an action, attached to an action when using Reduks modules
 * Created by daely on 7/31/2016.
 */
class ReduksContext(val moduleId:String) {
    infix operator fun plus(other: ReduksContext) = ReduksContext("$moduleId+${other.moduleId}")
}