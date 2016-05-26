package com.brianegan.bansaDevTools

class DevToolsState<S>(val computedStates: List<S> // List of computed states after committed.
                       ,
                       val stagedActions: List<Any?> // List of all currently staged actions.
                       ,
                       val currentPosition: Int? // Current state index in the computedStates List.
) {

    val committedState: S
        get() = computedStates[0]

    val currentAppState: S
        get() = computedStates[currentPosition!!]

    val currentAction: Any?
        get() = stagedActions[currentPosition!!]
}
