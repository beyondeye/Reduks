package com.beyondeye.reduks.logger.zjsonpatch;

//import com.fasterxml.jackson.core.JsonToken;
//import com.fasterxml.jackson.databind.JsonNode;
//import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.EnumMap;
import java.util.Map;

enum NodeType {
    /**
     * Array nodes
     */
    ARRAY("array"),
    /**
     * Boolean nodes
     */
    BOOLEAN("boolean"),
    /**
     * Integer nodes
     */
    INTEGER("integer"),
    /**
     * Number nodes (ie, decimal numbers)
     */
    NULL("null"),
    /**
     * Object nodes
     */
    NUMBER("number"),
    /**
     * Null nodes
     */
    OBJECT("object"),
    /**
     * String nodes
     */
    STRING("string");

    /**
     * The name for this type, as encountered in a JSON schema
     */
    private final String name;

    /* *ORIGINALCODE*
    private static final Map<JsonToken, NodeType> TOKEN_MAP
            = new EnumMap<JsonToken, NodeType>(JsonToken.class);

    static {
        TOKEN_MAP.put(JsonToken.START_ARRAY, ARRAY);
        TOKEN_MAP.put(JsonToken.VALUE_TRUE, BOOLEAN);
        TOKEN_MAP.put(JsonToken.VALUE_FALSE, BOOLEAN);
        TOKEN_MAP.put(JsonToken.VALUE_NUMBER_INT, INTEGER);
        TOKEN_MAP.put(JsonToken.VALUE_NUMBER_FLOAT, NUMBER);
        TOKEN_MAP.put(JsonToken.VALUE_NULL, NULL);
        TOKEN_MAP.put(JsonToken.START_OBJECT, OBJECT);
        TOKEN_MAP.put(JsonToken.VALUE_STRING, STRING);

    }
*/
    NodeType(final String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static NodeType getNodeType(final JsonObject node) {
        if(node.isJsonArray()) return ARRAY;
        if(node.isJsonNull()) return NULL;
        if(node.isJsonObject()) return OBJECT;
        //primitive
        JsonPrimitive pnode=node.getAsJsonPrimitive();
        if(pnode.isBoolean()) return BOOLEAN;
        //TODO need to distinguish between float and int
        if(pnode.isNumber()) return NUMBER;
        throw new IllegalArgumentException("Unhandled token type:"+ pnode.toString());
        /* *ORIGINALCODE*
        final JsonToken token = node.asToken();
        final NodeType ret = TOKEN_MAP.get(token);
        Preconditions.checkNotNull(ret, "unhandled token type " + token);
        return ret;
        */
    }
}
