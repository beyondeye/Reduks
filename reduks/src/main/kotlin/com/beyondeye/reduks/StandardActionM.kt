package com.beyondeye.reduks


/**
 * same as [StandardAction] but with additional field meta
 */
interface StandardActionM : StandardAction {
    val meta: Any?
}

//val StandardAction.started:Boolean get()= payload==null && !error
//val StandardAction.completed:Boolean get()= payload!=null && !error
