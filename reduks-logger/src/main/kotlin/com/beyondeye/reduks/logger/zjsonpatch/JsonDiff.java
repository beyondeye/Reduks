package com.beyondeye.reduks.logger.zjsonpatch;

//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.google.common.base.Function;
//import com.google.common.base.Joiner;
//import com.google.common.base.Preconditions;
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
import com.beyondeye.reduks.logger.zjsonpatch.lcs.ListUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

//import org.apache.commons.collections4.ListUtils;

import org.json.JSONArray;

import java.util.*;

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
public final class JsonDiff {

    private JsonDiff() {}


    public static JsonArray asJson(final JsonElement source, final JsonElement target) {
        final List<Diff> diffs = new ArrayList<Diff>();
        List<Object> path = new LinkedList<Object>();
        /**
         * generating diffs in the order of their occurrence
         */
        generateDiffs(diffs, path, source, target);
        /**
         * Merging remove & add to move operation
         */
        compactDiffs(diffs);

        return getJsonNodes(diffs);
    }

    /**
     * This method merge 2 diffs ( remove then add, or vice versa ) with same value into one Move operation,
     * all the core logic resides here only
     */
    private static void compactDiffs(List<Diff> diffs) {
        for (int i = 0; i < diffs.size(); i++) {
            Diff diff1 = diffs.get(i);

            // if not remove OR add, move to next diff
            if (!(Operation.REMOVE.equals(diff1.getOperation()) ||
                    Operation.ADD.equals(diff1.getOperation()))) {
                continue;
            }

            for (int j = i + 1; j < diffs.size(); j++) {
                Diff diff2 = diffs.get(j);
                if (!diff1.getValue().equals(diff2.getValue())) {
                    continue;
                }

                Diff moveDiff = null;
                if (Operation.REMOVE.equals(diff1.getOperation()) &&
                        Operation.ADD.equals(diff2.getOperation())) {
                    computeRelativePath(diff2.getPath(), i + 1, j - 1, diffs);
                    moveDiff = new Diff(Operation.MOVE, diff1.getPath(), diff2.getValue(), diff2.getPath());

                } else if (Operation.ADD.equals(diff1.getOperation()) &&
                        Operation.REMOVE.equals(diff2.getOperation())) {
                    computeRelativePath(diff2.getPath(), i, j - 1, diffs); // diff1's add should also be considered
                    moveDiff = new Diff(Operation.MOVE, diff2.getPath(), diff1.getValue(), diff1.getPath());
                }
                if (moveDiff != null) {
                    diffs.remove(j);
                    diffs.set(i, moveDiff);
                    break;
                }
            }
        }
    }

    //Note : only to be used for arrays
    //Finds the longest common Ancestor ending at Array
    private static void computeRelativePath(List<Object> path, int startIdx, int endIdx, List<Diff> diffs) {
        List<Integer> counters = new ArrayList<Integer>();

        resetCounters(counters, path.size());

        for (int i = startIdx; i <= endIdx; i++) {
            Diff diff = diffs.get(i);
            //Adjust relative path according to #ADD and #Remove
            if (Operation.ADD.equals(diff.getOperation()) || Operation.REMOVE.equals(diff.getOperation())) {
                updatePath(path, diff, counters);
            }
        }
        updatePathWithCounters(counters, path);
    }

    private static void resetCounters(List<Integer> counters, int size) {
        for (int i = 0; i < size; i++) {
            counters.add(0);
        }
    }

    private static void updatePathWithCounters(List<Integer> counters, List<Object> path) {
        for (int i = 0; i < counters.size(); i++) {
            int value = counters.get(i);
            if (value != 0) {
                Integer currValue = Integer.parseInt(path.get(i).toString());
                path.set(i, String.valueOf(currValue + value));
            }
        }
    }

    private static void updatePath(List<Object> path, Diff pseudo, List<Integer> counters) {
        //find longest common prefix of both the paths

        if (pseudo.getPath().size() <= path.size()) {
            int idx = -1;
            for (int i = 0; i < pseudo.getPath().size() - 1; i++) {
                if (pseudo.getPath().get(i).equals(path.get(i))) {
                    idx = i;
                } else {
                    break;
                }
            }
            if (idx == pseudo.getPath().size() - 2) {
                if (pseudo.getPath().get(pseudo.getPath().size() - 1) instanceof Integer) {
                    updateCounters(pseudo, pseudo.getPath().size() - 1, counters);
                }
            }
        }
    }

    private static void updateCounters(Diff pseudo, int idx, List<Integer> counters) {
        if (Operation.ADD.equals(pseudo.getOperation())) {
            counters.set(idx, counters.get(idx) - 1);
        } else {
            if (Operation.REMOVE.equals(pseudo.getOperation())) {
                counters.set(idx, counters.get(idx) + 1);
            }
        }
    }

    private static JsonArray getJsonNodes(List<Diff> diffs) {
//        JsonNodeFactory FACTORY = JsonNodeFactory.instance;
        final JsonArray patch = new JsonArray();
        for (Diff diff : diffs) {
            JsonObject jsonNode = getJsonNode( diff);
            patch.add(jsonNode);
        }
        return patch;
    }

    private static JsonObject getJsonNode( Diff diff) {
        JsonObject jsonNode = new JsonObject();
        jsonNode.addProperty(Constants.OP, diff.getOperation().rfcName());
        jsonNode.addProperty(Constants.PATH, getArrayNodeRepresentation(diff.getPath()));
        if (Operation.MOVE.equals(diff.getOperation())) {
            jsonNode.addProperty(Constants.FROM, getArrayNodeRepresentation(diff.getPath())); //required {from} only in case of Move Operation
            jsonNode.addProperty(Constants.PATH, getArrayNodeRepresentation(diff.getToPath()));  // destination Path
        }
        if (!Operation.REMOVE.equals(diff.getOperation()) && !Operation.MOVE.equals(diff.getOperation())) { // setting only for Non-Remove operation
            jsonNode.add(Constants.VALUE, diff.getValue());
        }
        return jsonNode;
    }

    public static String  EncodePath(Object object) {
        String path = object.toString(); // see http://tools.ietf.org/html/rfc6901#section-4
        return path.replaceAll("~", "~0").replaceAll("/", "~1");
    }
    //join path parts in argument 'path', inserting a '/' between joined elements, starting with '/' and transforming the element of the list with ENCODE_PATH_FUNCTION
    private static String getArrayNodeRepresentation(List<Object> path) {
//        return Joiner.on('/').appendTo(new StringBuilder().append('/'),
//                Iterables.transform(path, ENCODE_PATH_FUNCTION)).toString();
        StringBuilder sb=new StringBuilder();
        for(int i=0;i<path.size();++i) {
            sb.append('/');
            sb.append(EncodePath(path.get(i)));

        }
        return sb.toString();
    }

    private static void generateDiffs(List<Diff> diffs, List<Object> path, JsonElement source, JsonElement target) {
        if (!source.equals(target)) {
            final int sourceType = NodeType.getNodeType(source);
            final int targetType = NodeType.getNodeType(target);

            if (sourceType == NodeType.ARRAY && targetType == NodeType.ARRAY) {
                //both are arrays
                compareArray(diffs, path, source, target);
            } else if (sourceType == NodeType.OBJECT && targetType == NodeType.OBJECT) {
                //both are json
                compareObjects(diffs, path, source.getAsJsonObject(), target.getAsJsonObject());
            } else {
                //can be replaced

                diffs.add(Diff.generateDiff(Operation.REPLACE, path, target));
            }
        }
    }

    private static void compareArray(List<Diff> diffs, List<Object> path, JsonElement source_, JsonElement target_) {
        JsonArray source=source_.getAsJsonArray();
        JsonArray target=target_.getAsJsonArray();
        List<JsonElement> lcs = getLCS(source, target);
        int srcIdx = 0;
        int targetIdx = 0;
        int lcsIdx = 0;
        int srcSize = source.size();
        int targetSize = target.size();
        int lcsSize = lcs.size();

        int pos = 0;
        while (lcsIdx < lcsSize) {
            JsonElement lcsNode = lcs.get(lcsIdx);
            JsonElement srcNode = source.get(srcIdx);
            JsonElement targetNode = target.get(targetIdx);


            if (lcsNode.equals(srcNode) && lcsNode.equals(targetNode)) { // Both are same as lcs node, nothing to do here
                srcIdx++;
                targetIdx++;
                lcsIdx++;
                pos++;
            } else {
                if (lcsNode.equals(srcNode)) { // src node is same as lcs, but not targetNode
                    //addition
                    List<Object> currPath = getPath(path, pos);
                    diffs.add(Diff.generateDiff(Operation.ADD, currPath, targetNode));
                    pos++;
                    targetIdx++;
                } else if (lcsNode.equals(targetNode)) { //targetNode node is same as lcs, but not src
                    //removal,
                    List<Object> currPath = getPath(path, pos);
                    diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, srcNode));
                    srcIdx++;
                } else {
                    List<Object> currPath = getPath(path, pos);
                    //both are unequal to lcs node
                    generateDiffs(diffs, currPath, srcNode, targetNode);
                    srcIdx++;
                    targetIdx++;
                    pos++;
                }
            }
        }

        while ((srcIdx < srcSize) && (targetIdx < targetSize)) {
            JsonElement srcNode = source.get(srcIdx);
            JsonElement targetNode = target.get(targetIdx);
            List<Object> currPath = getPath(path, pos);
            generateDiffs(diffs, currPath, srcNode, targetNode);
            srcIdx++;
            targetIdx++;
            pos++;
        }
        pos = addRemaining(diffs, path, target, pos, targetIdx, targetSize);
        removeRemaining(diffs, path, pos, srcIdx, srcSize, source);
    }

    private static Integer removeRemaining(List<Diff> diffs, List<Object> path, int pos, int srcIdx, int srcSize, JsonElement source_) {
        JsonArray source = source_.getAsJsonArray();
        while (srcIdx < srcSize) {
            List<Object> currPath = getPath(path, pos);
            diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.get(srcIdx)));
            srcIdx++;
        }
        return pos;
    }

    private static Integer addRemaining(List<Diff> diffs, List<Object> path, JsonElement target_, int pos, int targetIdx, int targetSize) {
        JsonArray target=target_.getAsJsonArray();
        while (targetIdx < targetSize) {
            JsonElement jsonNode = target.get(targetIdx);
            List<Object> currPath = getPath(path, pos);
            diffs.add(Diff.generateDiff(Operation.ADD, currPath, jsonNode.deepCopy()));
            pos++;
            targetIdx++;
        }
        return pos;
    }

    private static void compareObjects(List<Diff> diffs, List<Object> path, JsonObject source, JsonObject target) {
        Iterator<Map.Entry<String, JsonElement>> keysFromSrc = source.entrySet().iterator();
        while (keysFromSrc.hasNext()) {
            String key = keysFromSrc.next().getKey();
            if (!target.has(key)) {
                //remove case
                List<Object> currPath = getPath(path, key);
                diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.get(key)));
                continue;
            }
            List<Object> currPath = getPath(path, key);
            generateDiffs(diffs, currPath, source.get(key), target.get(key));
        }
        Iterator<Map.Entry<String, JsonElement>> keysFromTarget = target.entrySet().iterator();
        while (keysFromTarget.hasNext()) {
            String key = keysFromTarget.next().getKey();
            if (!source.has(key)) {
                //add case
                List<Object> currPath = getPath(path, key);
                diffs.add(Diff.generateDiff(Operation.ADD, currPath, target.get(key)));
            }
        }
    }

    private static List<Object> getPath(List<Object> path, Object key) {
        List<Object> toReturn = new ArrayList<Object>();
        toReturn.addAll(path);
        toReturn.add(key);
        return toReturn;
    }

    private static List<JsonElement> getLCS(final JsonElement first_, final JsonElement second_) {
        if(!first_.isJsonArray()) throw new IllegalArgumentException("LCS can only work on JSON arrays");
        if(!second_.isJsonArray()) throw new IllegalArgumentException("LCS can only work on JSON arrays");
        JsonArray first = (JsonArray) first_;
        JsonArray second = (JsonArray) second_;

        return ListUtils.longestCommonSubsequence(Lists.newArrayList(first), Lists.newArrayList(second));
    }
}
