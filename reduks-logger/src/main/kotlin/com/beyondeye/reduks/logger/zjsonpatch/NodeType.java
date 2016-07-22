package com.beyondeye.reduks.logger.zjsonpatch;

import com.google.gson.JsonElement;


class NodeType {
    static final int ARRAY=1;
    static final int OBJECT=2;
//    static final int NULL=3;
    static final int PRIMITIVE_OR_NULL=3;

    public static int getNodeType(final JsonElement node) {
        if(node.isJsonArray()) return ARRAY;
        if(node.isJsonObject()) return OBJECT;
//        if(node.isJsonNull()) return NULL;
        return PRIMITIVE_OR_NULL;
    }
}
