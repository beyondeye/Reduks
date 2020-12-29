package com.beyondeye.reduks

/**
 * In reduks we "react" to changes of the reduks state and perform actions.
 * If there is a sequence of actions, then we listen for the changes in the state that
 * trigger each step in the sequence SEPARATELY and it can be difficult to understand/mantain
 * such chain of actions, or even understand that  such chain exists at all,unless we analyze
 * thoroughly each of the onChangeIn clauses in a [StoreSubscriber] code
 * the [StepInSequence] class purpose is to make this chain of action more explicit and easier to
 * identify, and also help to avoid triggering by mistake a step of the chain before the previous
 * steps in the chain have been completed.
 * How to use: for each of such logic sequences that exist in your application, define a [StepInSequence]
 * field in the reduks state. Then, instead of listening to change in the reduks state with
 * [AbstractSelector.onChangeIn], use instead [AbstractSelector.onChangeAtStep]
 */
class StepInSequence(val nsteps:Int=1000, val curstep:Int=-1) {
    /**
     * use this method in State reducer, to restart the [StepInSequence]
     */
    fun restarted(startStep:Int=0) = StepInSequence(nsteps,startStep)
    /**
     * use this method in State reducer, to set the  [StepInSequence] as completed
     */
    fun completed()=StepInSequence(nsteps,nsteps)
    /**
     * use this method in State reducer, to advance to next step in the [StepInSequence]
     */
    fun withNextStep() = StepInSequence(nsteps,(curstep+1).coerceAtMost(nsteps))
    fun withStep(newstep:Int)=StepInSequence(nsteps,newstep.coerceAtMost(nsteps))
    fun isStarted() = curstep>=0
    fun isCompleted()= curstep==nsteps
    fun isRunning()= curstep>=0 && curstep<nsteps
}