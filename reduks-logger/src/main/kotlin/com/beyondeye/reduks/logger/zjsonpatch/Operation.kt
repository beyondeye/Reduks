package com.flipkart.zjsonpatch

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
internal enum class Operation private constructor(private val rfcName: String) {
    ADD("add"),
    REMOVE("remove"),
    REPLACE("replace"),
    MOVE("move");

    fun rfcName(): String {
        return this.rfcName
    }

    companion object {
        private val OPS = mapOf(
                ADD.rfcName to ADD,
                REMOVE.rfcName to REMOVE,
                REPLACE.rfcName to REPLACE,
                MOVE.rfcName to MOVE)

        fun fromRfcName(rfcName: String): Operation {
            checkNotNull(rfcName,{"rfcName cannot be null"} )
            return checkNotNull(OPS.get(rfcName.toLowerCase()), {"unknown / unsupported operation $rfcName"})
        }
    }


}
