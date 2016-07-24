package com.beyondeye.zjsonpatch

import com.google.gson.JsonElement

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
internal class Diff {
    val operation: Int
    val path: MutableList<Any>
    val value: JsonElement
    val toPath: List<Any>? //only to be used in move operation

    constructor(operation: Int, path: List<Any>, value: JsonElement) {
        this.operation = operation
        this.path = path.toMutableList()
        this.toPath=null
        this.value = value
    }

    constructor(operation: Int, fromPath: List<Any>, value: JsonElement, toPath: List<Any>) {
        this.operation = operation
        this.path = fromPath.toMutableList()
        this.value = value
        this.toPath = toPath
    }

    companion object {

        fun generateDiff(replace: Int, path: List<Any>, target: JsonElement): Diff {
            return Diff(replace, path, target)
        }
    }
}
