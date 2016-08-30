package com.beyondeye.reduks

/**
 * inspired by https://github.com/acdlite/flux-standard-action
 * We don't have a 'type' field like in the original FSA because in Kotlin/Java is much more efficient
 * to directly check the class type, and also considering kotlin sealed class hierarchies, that help writing reducers
 * see https://kotlinlang.org/docs/reference/classes.html#sealed-classes
 */
interface StandardAction : Action {
    val payload: Any?
    val error: Boolean
}

//val StandardAction.started:Boolean get()= payload==null && !error
//val StandardAction.completed:Boolean get()= payload!=null && !error
