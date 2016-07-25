package com.beyondeye.reduks.logger

//class DefaultColorObject:ColorObject {
//    override val title: String = "#000000"
//    override val prevState: String = "#9E9E9E"
//    override val action: String = "#03A9F4"
//    override val nextState: String= "#4CAF50"
//    override val error: String= "#F20404"
//}

/**
 *
 * Created by daely on 7/21/2016.
 */
data class Options<S>(
        val level:(logElement:Int,Action:Any,prevState:S,nextState:S?,error:Throwable?)->Int?={le,a,ps,ns,e-> LogLevel.DEBUG }, // get log level as a function of log element and current log entry data
        val logActionDuration:Boolean=false, // Print the duration of each action?
        val logErrors:Boolean=true, // Should the logger catch, log, and re-throw errors?
        val collapsed:(state:S?,action:Any)->Boolean={state,action -> false}, // Takes a boolean or optionally a function that receives `getState` function for accessing current store state and `action` object as parameters. Returns `true` if the log group should be collapsed, `false` otherwise.
        val predicate:(state:S,action:Any)->Boolean={state,action -> true}, // If specified this function will be called before each action is processed with this middleware.
        val stateTransformer:(S)->S={s->s}, // Transform state before print. Eg. convert Immutable object to plain JSON.
        val actionTransformer:(Any)->Any={a->a}, // Transform action before print.
        val errorTransformer:(Throwable)->Throwable = {e->e}, // Transform error before print.
        val actionTypeExtractor:(Any)->String = {a->a.javaClass.simpleName},
        val logStateDiff:Boolean = false, // Show diff between states.
        val diffPredicate:((state:S,action:Any)->Boolean)?=null // Filter function for showing states diff.'
)