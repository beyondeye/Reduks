package com.beyondeye.reduks.modules

import com.beyondeye.reduks.Action

/**
 * Action object with attached ActionContext: this wrapped action is used when combining multiple Reduks modules
 * Created by daely on 7/31/2016.
 */
class ActionWithContext(action_: Any, context_: ReduksContext):Action {
    val action: Any
    val context: ReduksContext
    init {
        if(action_ !is ActionWithContext) {
            action=action_
            context=context_
        } else {
            action=action_.action
            context=context_/action_.context
        }
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        //------
        //the following lines allow custom matchA with action pattern
        if(other is ActionWithContextPattern)
            return other.match(this)
        //------
        if (other?.javaClass != javaClass) return false

        other as ActionWithContext

        if (action != other.action) return false
        if (context != other.context) return false

        return true
    }

    override fun hashCode(): Int {
        var result = action.hashCode()
        result = 31 * result + context.hashCode()
        return result
    }
}

// see https://programmingideaswithjake.wordpress.com/2016/08/27/improved-pattern-matching-in-kotlin/
/**
 * classes that implement this interface can be used for matching  [ActionWithContext] objects in when(action) {} statements,
 * thanks to the hook inserted above in [ActionWithContext.equals]
 */
interface ActionWithContextPattern {
    fun match(a: ActionWithContext): Boolean
}

class ActionWithContextLambdaPattern(val match_ctx: ReduksContext, val matchfn: (Any) -> Boolean): ActionWithContextPattern {
    override fun match(a: ActionWithContext): Boolean {
        if(a.context!=match_ctx) return false
        return matchfn(a.action)
    }
}

/**
 *  * use this to match an [ActionWithContext] with [ReduksContext] equal to 'this' and action
 *  that when put as input of matchfn return true
 *  sample usage: when(a) {
 *      ctx2match.matchA{ it is SomeActionType } -> //...do something
 *  }
 */
fun ReduksContext.matchA(matchfn: (Any) -> Boolean) = ActionWithContextLambdaPattern(this, matchfn)

/**
 * use this to match an [ActionWithContext] with embedded action of type T and context equal to
 * this
 * sample usage: when(a) {
 *   ctx.isA<SomeActionType>() -> //...do something
 * }
 *
 */
inline fun <reified T> ReduksContext.isA() = ActionWithContextLambdaPattern(this) { it -> it is T }