package com.beyondeye.reduks.logger.zjsonpatch;

import com.google.gson.JsonElement;

import java.util.List;

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
class Diff {
    private final Operation operation;
    private final List<Object> path;
    private final JsonElement value;
    private List<Object> toPath; //only to be used in move operation

    Diff(Operation operation, List<Object> path, JsonElement value) {
        this.operation = operation;
        this.path = path;
        this.value = value;
    }

    Diff(Operation operation, List<Object> fromPath, JsonElement value, List<Object> toPath) {
        this.operation = operation;
        this.path = fromPath;
        this.value = value;
        this.toPath = toPath;
    }

    public Operation getOperation() {
        return operation;
    }

    public List<Object> getPath() {
        return path;
    }

    public JsonElement getValue() {
        return value;
    }

    public static Diff generateDiff(Operation replace, List<Object> path, JsonElement target) {
        return new Diff(replace, path, target);
    }

    List<Object> getToPath() {
        return toPath;
    }
}
