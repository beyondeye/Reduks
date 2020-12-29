package com.beyondeye.kjsonpatch

import com.google.gson.JsonElement


internal object NodeType {
    val ARRAY = 1
    val OBJECT = 2
    //    static final int NULL=3;
    val PRIMITIVE_OR_NULL = 3

    fun getNodeType(node: JsonElement): Int {
        if (node.isJsonArray) return ARRAY
        if (node.isJsonObject) return OBJECT
        //        if(node.isJsonNull()) return NULL;
        return PRIMITIVE_OR_NULL
    }
}
