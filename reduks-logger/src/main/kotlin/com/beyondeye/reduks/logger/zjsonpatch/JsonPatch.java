package com.beyondeye.reduks.logger.zjsonpatch;

//import com.google.common.base.Function;
//import com.google.common.base.Splitter;
//import com.google.common.base.Strings;
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

import kotlin.NotImplementedError;

/**
 * User: gopi.vishwakarma
 * Date: 31/07/14
 *
 *  * use Gson instead of Jackson, converted to Kotlin and some clean up
 * Dario Elyasy 22/7/2016
 */
public final class JsonPatch {
    private JsonPatch() {}

    public static JsonElement apply(JsonArray patch, JsonElement source) {
        Iterator<JsonElement> operations = patch.iterator();
        JsonElement ret = source.deepCopy();
        while (operations.hasNext()) {
            JsonObject jsonNode = (JsonObject) operations.next();
            Operation operation = Operation.fromRfcName(jsonNode.get(Constants.OP).toString().replaceAll("\"", ""));
            List<String> path = getPath(jsonNode.get(Constants.PATH));
            List<String> fromPath = null;
            if (Operation.MOVE.equals(operation)) {
                fromPath = getPath(jsonNode.get(Constants.FROM));
            }
            JsonElement value = null;
            if (!Operation.REMOVE.equals(operation) && !Operation.MOVE.equals(operation)) {
                value = jsonNode.get(Constants.VALUE);
            }

            switch (operation) {
                case REMOVE:
                    remove(ret, path);
                    break;
                case REPLACE:
                    ret = replace(ret, path, value);
                    break;
                case ADD:
                    ret = add(ret, path, value);
                    break;
                case MOVE:
                    ret = move(ret, fromPath, path);
                    break;
            }
        }
        return ret;
    }

    private static JsonElement move(JsonElement node, List<String> fromPath, List<String> toPath) {
        JsonElement parentNode = getParentNode(node, fromPath);
        String field = fromPath.get(fromPath.size() - 1).replaceAll("\"", "");
//        JsonElement valueNode =  parentNode.isJsonArray() ? parentNode.get(Integer.parseInt(field)) : parentNode.get(field);
        JsonElement valueNode;
        if(parentNode.isJsonArray())
            valueNode=((JsonArray)parentNode).get(Integer.parseInt(field));
        else
            valueNode=((JsonObject)parentNode).get(field);
        remove(node, fromPath);
        return add(node, toPath, valueNode);
    }

    private static JsonElement add(JsonElement node, List<String> path, JsonElement value) {
        if (path.isEmpty()) {
            throw new RuntimeException("[ADD Operation] path is empty , path : ");
        } else {
            JsonElement parentNode = getParentNode(node, path);
            if (parentNode == null) {
                throw new RuntimeException("[ADD Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToReplace = path.get(path.size() - 1).replaceAll("\"", "");
                if (fieldToReplace.equals("") && path.size() == 1) {
                    return value;
                }
                if (!isContainerNode(parentNode))  //not array or object node
                {
                    throw new RuntimeException("[ADD Operation] parent is not a container in source, path provided : " + path + " | node : " + parentNode);
                } else {
                    if (parentNode.isJsonArray()) {
                        addToArray(path, value, parentNode);
                    } else {
                        addToObject(path, parentNode, value);
                    }
                }
            }
        }
        return node;
    }

    private static boolean isContainerNode(JsonElement parentNode) {
        return parentNode.isJsonObject()||parentNode.isJsonArray();
    }

    private static void addToObject(List<String> path, JsonElement node, JsonElement value) {
        final JsonObject target = (JsonObject) node;
        String key = path.get(path.size() - 1).replaceAll("\"", "");
        target.add(key, value);
    }

    private static void addToArray(List<String> path, JsonElement value, JsonElement parentNode) {
        final JsonArray target = (JsonArray) parentNode;
        String idxStr = path.get(path.size() - 1);

        if ("-".equals(idxStr)) {
            // see http://tools.ietf.org/html/rfc6902#section-4.1
            target.add(value);
        } else {
            Integer idx = Integer.parseInt(idxStr.replaceAll("\"", ""));
            if (idx < target.size()) {
                insert(target,idx, value);
            } else {
                if (idx == target.size()) {
                    target.add(value);
                } else {
                    throw new RuntimeException("[ADD Operation] [addToArray] index Out of bound, index provided is higher than allowed, path " + path);
                }
            }
        }
    }

    private static void insert(JsonArray target, Integer idx, JsonElement value) {
        throw new NotImplementedError();
    }

    private static JsonElement replace(JsonElement node, List<String> path, JsonElement value) {
        if (path.isEmpty()) {
            throw new RuntimeException("[Replace Operation] path is empty");
        } else {
            JsonElement parentNode = getParentNode(node, path);
            if (parentNode == null) {
                throw new RuntimeException("[Replace Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToReplace = path.get(path.size() - 1).replaceAll("\"", "");
                if (isNullOrEmpty(fieldToReplace) && path.size() == 1) {
                    return value;
                }
                if (parentNode.isJsonObject())
                    ((JsonObject) parentNode).add(fieldToReplace, value);
                else
                    ((JsonArray) parentNode).set(Integer.parseInt(fieldToReplace), value);
            }
            return node;
        }
    }

    private static boolean isNullOrEmpty(String s) {
        return (s==null ||s.length()==0);
    }

    private static void remove(JsonElement node, List<String> path) {
        if (path.isEmpty()) {
            throw new RuntimeException("[Remove Operation] path is empty");
        } else {
            JsonElement parentNode = getParentNode(node, path);
            if (parentNode == null) {
                throw new RuntimeException("[Remove Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToRemove = path.get(path.size() - 1).replaceAll("\"", "");
                if (parentNode.isJsonObject())
                    ((JsonObject) parentNode).remove(fieldToRemove);
                else
                    ((JsonArray) parentNode).remove(Integer.parseInt(fieldToRemove));
            }
        }
    }

    private static JsonElement getParentNode(JsonElement node, List<String> fromPath) {
        List<String> pathToParent = fromPath.subList(0, fromPath.size() - 1); // would never by out of bound, lets see
        return getNode(node, pathToParent, 1);
    }

    private static JsonElement getNode(JsonElement ret, List<String> path, int pos) {
        if (pos >= path.size()) {
            return ret;
        }
        String key = path.get(pos);
        if (ret.isJsonArray()) {
            int keyInt = Integer.parseInt(key.replaceAll("\"", ""));
            return getNode(((JsonArray)ret).get(keyInt), path, ++pos);
        } else if (ret.isJsonObject()) {
            if (((JsonObject)ret).has(key)) {
                return getNode(((JsonObject)ret).get(key), path, ++pos);
            }
            return null;
        } else {
            return ret;
        }
    }

    String DecodePath(String path) {
        return path.replaceAll("~1", "/").replaceAll("~0", "~"); // see http://tools.ietf.org/html/rfc6901#section-4
    }

    private static List<String> getPath(JsonElement path) {
        throw new NotImplementedError();
//        List<String> paths = Splitter.on('/').splitToList(path.toString().replaceAll("\"", ""));
//        return Lists.newArrayList(Iterables.transform(paths, DECODE_PATH_FUNCTION));
    }
}
