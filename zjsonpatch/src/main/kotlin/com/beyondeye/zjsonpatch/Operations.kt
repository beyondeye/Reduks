package com.beyondeye.zjsonpatch

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
internal class Operations {
    companion object {
        val ADD:Int=0
        val REMOVE:Int=1
        val REPLACE:Int=2
        val MOVE:Int=3
        val ADD_name="add"
        val REMOVE_name="remove"
        val REPLACE_name="replace"
        val MOVE_name="move"
        private val OPS = mapOf(
                ADD_name to ADD,
                REMOVE_name to REMOVE,
                REPLACE_name to REPLACE,
                MOVE_name to MOVE)
        private val NAMES = mapOf(
                ADD to ADD_name,
                REMOVE to REMOVE_name,
                REPLACE to REPLACE_name,
                MOVE to MOVE_name)

        @JvmStatic fun opFromName(rfcName: String): Int {
            return checkNotNull(OPS.get(rfcName.toLowerCase()), {"unknown / unsupported operation $rfcName"})
        }
        @JvmStatic fun nameFromOp(operation:Int): String {
            return checkNotNull(NAMES.get(operation), {"unknown / unsupported operation $operation"})
        }
    }


}
