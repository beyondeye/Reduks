package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.Reducer


class DevToolsReducer<S>(private val appReducer: Reducer<S>) : Reducer<DevToolsState<S>> {

    override fun reduce(state: DevToolsState<S>, action: Any): DevToolsState<S> {
        if (action !is DevToolsAction) {
            throw IllegalArgumentException("When using the Dev Tools, all actions must be wrapped as a om.beyondeye.reduksDevTools.DevToolsAction")
        }

        when (action.type) {
            DevToolsAction.INIT -> {
                val initialState = appReducer.reduce(state.currentAppState, action)

                return DevToolsState(
                        listOf(initialState),
                        listOf<Any>(action),
                        0)
            }

            DevToolsAction.PERFORM_ACTION -> {
                val addToEnd = state.currentPosition === state.computedStates.size - 1

                return performAction(
                        state,
                        action,
                        if (addToEnd) state.computedStates else state.computedStates.subList(0, state.currentPosition!! + 1),
                        if (addToEnd) state.stagedActions else state.stagedActions.subList(0, state.currentPosition!! + 1))
            }

            DevToolsAction.RESET -> return DevToolsState(
                    listOf(state.committedState),
                    listOf<Any>(action),
                    0)

            DevToolsAction.SAVE -> return DevToolsState(
                    listOf(state.currentAppState),
                    listOf<Any>(action),
                    0)

            DevToolsAction.JUMP_TO_STATE -> return DevToolsState(
                    state.computedStates,
                    state.stagedActions,
                    action.getPosition())

            DevToolsAction.RECOMPUTE -> return DevToolsState(
                    recomputeStates(state.computedStates, state.stagedActions),
                    state.stagedActions,
                    state.stagedActions.size - 1)

            else -> return state
        }
    }

    private fun performAction(state: DevToolsState<S>,
                              devToolsAction: DevToolsAction,
                              computedStates: List<S>,
                              stagedActions: List<Any?>): DevToolsState<S> {
        val newStates = computedStates.toMutableList()
        val newActions =stagedActions.toMutableList()

        newStates.add(appReducer.reduce(state.currentAppState, devToolsAction.appAction!!))
        newActions.add(devToolsAction.appAction)

        return DevToolsState(
                newStates,
                newActions,
                newStates.size - 1)
    }

    private fun recomputeStates(computedStates: List<S>, stagedActions: List<Any?>): List<S> {
        val recomputedStates = mutableListOf<S>()
        var currentState = computedStates[0]

        for (i in computedStates.indices) {
            val currentAction = stagedActions[i]
            currentState = appReducer.reduce(currentState, currentAction!!)
            recomputedStates.add(currentState)
        }

        return recomputedStates
    }
}
