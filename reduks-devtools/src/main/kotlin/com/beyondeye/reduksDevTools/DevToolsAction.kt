package com.beyondeye.reduksDevTools

import com.beyondeye.reduks.Action


class DevToolsAction private constructor(val type: String,
                                         val appAction: Any?,
                                         private val position: Int?) : Action {

    fun getPosition(): Int {
        return position!!
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if(other==null) return false
        if(other !is DevToolsAction) return false

        if(type  !=other.type) return false
        if(appAction!=other.appAction) return false
        if(position!=other.position) return false
        return true
    }

    override fun hashCode(): Int {
        var result =  type.hashCode()
        result = 31 * result + if (appAction != null) appAction.hashCode() else 0
        result = 31 * result + if (position != null) position.hashCode() else 0
        return result
    }

    companion object {
        val PERFORM_ACTION = "PERFORM_ACTION"
        val JUMP_TO_STATE = "JUMP_TO_STATE"
        val SAVE = "SAVE"
        val RESET = "RESET"
        val RECOMPUTE = "RECOMPUTE"
        val TOGGLE_ACTION = "TOGGLE_ACTION"
        internal val INIT = "INIT"

        fun createPerformAction(appAction: Any): DevToolsAction {
            return DevToolsAction(PERFORM_ACTION, appAction, null)
        }

        fun createJumpToStateAction(index: Int): DevToolsAction {
            return DevToolsAction(JUMP_TO_STATE, null, index)
        }

        fun createSaveAction(): DevToolsAction {
            return DevToolsAction(SAVE, null, null)
        }

        fun createResetAction(): DevToolsAction {
            return DevToolsAction(RESET, null, null)
        }

        fun createRecomputeAction(): DevToolsAction {
            return DevToolsAction(RECOMPUTE, null, null)
        }

        internal fun createInitAction(): DevToolsAction {
            return DevToolsAction(INIT, null, null)
        }
    }
}
