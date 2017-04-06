/*
 * Copyright 2016 flipkart.com zjsonpatch.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package com.beyondeye.zjsonpatch;


import com.beyondeye.zjsonpatch.lcs.ListUtils;
import com.google.gson.JsonElement;

import java.util.*;

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
public final class JsonDiff2 {
    static Operations op= new Operations();
    static Constants  consts = new Constants();
    private static final EncodePathFunction ENCODE_PATH_FUNCTION = new EncodePathFunction();

    private JsonDiff() {
    }

    private final static class EncodePathFunction implements Function<Object, String> {
        @Override
        public String apply(Object object) {
            String path = object.toString(); // see http://tools.ietf.org/html/rfc6901#section-4
            return path.replaceAll("~", "~0").replaceAll("/", "~1");
        }
    }

    public static JsonElement asJson(final JsonElement source, final JsonElement target) {
        final List<Diff2> diffs = new ArrayList<Diff2>();
        List<Object> path = new LinkedList<Object>();
        /*
         * generating diffs in the order of their occurrence
         */
        generateDiffs(diffs, path, source, target);
        /*
         * Merging remove & add to move operation
         */
        compactDiffs(diffs);
        /*
         * Introduce copy operation
         */
        introduceCopyOperation(source, target, diffs);

        return getJsonNodes(diffs);
    }

    private static List<Object> getMatchingValuePath(Map<JsonElement, List<Object>> unchangedValues, JsonElement value) {
        return unchangedValues.get(value);
    }

    private static void introduceCopyOperation(JsonElement source, JsonElement target, List<Diff2> diffs) {
        Map<JsonElement, List<Object>> unchangedValues = getUnchangedPart(source, target);
        for (int i = 0; i < diffs.size(); i++) {
            Diff2 diff = diffs.get(i);
            if (op.ADD.equals(diff.getOperation())) {
                List<Object> matchingValuePath = getMatchingValuePath(unchangedValues, diff.getValue());
                if (matchingValuePath != null) {
                    diffs.set(i, new Diff2(op.COPY, matchingValuePath, diff.getPath()));
                }
            }
        }
    }

    private static Map<JsonElement, List<Object>> getUnchangedPart(JsonElement source, JsonElement target) {
        Map<JsonElement, List<Object>> unchangedValues = new HashMap<JsonElement, List<Object>>();
        computeUnchangedValues(unchangedValues, Lists.newArrayList(), source, target);
        return unchangedValues;
    }

    private static void computeUnchangedValues(Map<JsonElement, List<Object>> unchangedValues, List<Object> path, JsonElement source, JsonElement target) {
        if (source.equals(target)) {
            unchangedValues.put(target, path);
            return;
        }

        final NodeType firstType = NodeType.getNodeType(source);
        final NodeType secondType = NodeType.getNodeType(target);

        if (firstType == secondType) {
            switch (firstType) {
                case OBJECT:
                    computeObject(unchangedValues, path, source, target);
                    break;
                case ARRAY:
                    computeArray(unchangedValues, path, source, target);
                default:
                /* nothing */
            }
        }
    }

    private static void computeArray(Map<JsonElement, List<Object>> unchangedValues, List<Object> path, JsonElement source, JsonElement target) {
        final int size = Math.min(source.size(), target.size());

        for (int i = 0; i < size; i++) {
            List<Object> currPath = getPath(path, i);
            computeUnchangedValues(unchangedValues, currPath, source.get(i), target.get(i));
        }
    }

    private static void computeObject(Map<JsonElement, List<Object>> unchangedValues, List<Object> path, JsonElement source, JsonElement target) {
        final Iterator<String> firstFields = source.fieldNames();
        while (firstFields.hasNext()) {
            String name = firstFields.next();
            if (target.has(name)) {
                List<Object> currPath = getPath(path, name);
                computeUnchangedValues(unchangedValues, currPath, source.get(name), target.get(name));
            }
        }
    }

    /**
     * This method merge 2 diffs ( remove then add, or vice versa ) with same value into one Move operation,
     * all the core logic resides here only
     */
    private static void compactDiffs(List<Diff2> diffs) {
        for (int i = 0; i < diffs.size(); i++) {
            Diff2 diff1 = diffs.get(i);

            // if not remove OR add, move to next diff
            if (!(op.REMOVE.equals(diff1.getOperation()) ||
                    Operation.ADD.equals(diff1.getOperation()))) {
                continue;
            }

            for (int j = i + 1; j < diffs.size(); j++) {
                Diff2 diff2 = diffs.get(j);
                if (!diff1.getValue().equals(diff2.getValue())) {
                    continue;
                }

                Diff2 moveDiff = null;
                if (op.REMOVE.equals(diff1.getOperation()) &&
                        op.ADD.equals(diff2.getOperation())) {
                    computeRelativePath(diff2.getPath(), i + 1, j - 1, diffs);
                    moveDiff = new Diff2(op.MOVE, diff1.getPath(), diff2.getPath());

                } else if (op.ADD.equals(diff1.getOperation()) &&
                        op.REMOVE.equals(diff2.getOperation())) {
                    computeRelativePath(diff2.getPath(), i, j - 1, diffs); // diff1's add should also be considered
                    moveDiff = new Diff2(op.MOVE, diff2.getPath(), diff1.getPath());
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
    private static void computeRelativePath(List<Object> path, int startIdx, int endIdx, List<Diff2> diffs) {
        List<Integer> counters = new ArrayList<Integer>();

        resetCounters(counters, path.size());

        for (int i = startIdx; i <= endIdx; i++) {
            Diff2 diff = diffs.get(i);
            //Adjust relative path according to #ADD and #Remove
            if (op.ADD.equals(diff.getOperation()) || op.REMOVE.equals(diff.getOperation())) {
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

    private static void updatePath(List<Object> path, Diff2 pseudo, List<Integer> counters) {
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

    private static void updateCounters(Diff2 pseudo, int idx, List<Integer> counters) {
        if (op.ADD.equals(pseudo.getOperation())) {
            counters.set(idx, counters.get(idx) - 1);
        } else {
            if (op.REMOVE.equals(pseudo.getOperation())) {
                counters.set(idx, counters.get(idx) + 1);
            }
        }
    }

    private static ArrayNode getJsonNodes(List<Diff2> diffs) {
        JsonNodeFactory FACTORY = JsonNodeFactory.instance;
        final ArrayNode patch = FACTORY.arrayNode();
        for (Diff2 diff : diffs) {
            ObjectNode jsonNode = getJsonNode(FACTORY, diff);
            patch.add(jsonNode);
        }
        return patch;
    }

    private static ObjectNode getJsonNode(JsonNodeFactory FACTORY, Diff2 diff) {
        ObjectNode jsonNode = FACTORY.objectNode();
        jsonNode.put(Constants.OP, diff.getOperation().rfcName());
        if (op.MOVE.equals(diff.getOperation()) || op.COPY.equals(diff.getOperation())) {
            jsonNode.put(Constants.FROM, getArrayNodeRepresentation(diff.getPath())); //required {from} only in case of Move Operation
            jsonNode.put(Constants.PATH, getArrayNodeRepresentation(diff.getToPath()));  // destination Path
        } else {
            jsonNode.put(Constants.PATH, getArrayNodeRepresentation(diff.getPath()));
            jsonNode.set(Constants.VALUE, diff.getValue());
        }
        return jsonNode;
    }

    private static String getArrayNodeRepresentation(List<Object> path) {
        return Joiner.on('/').appendTo(new StringBuilder().append('/'),
                Iterables.transform(path, ENCODE_PATH_FUNCTION)).toString();
    }


    private static void generateDiffs(List<Diff2> diffs, List<Object> path, JsonNode source, JsonNode target) {
        if (!source.equals(target)) {
            final NodeType sourceType = NodeType.getNodeType(source);
            final NodeType targetType = NodeType.getNodeType(target);

            if (sourceType == NodeType.ARRAY && targetType == NodeType.ARRAY) {
                //both are arrays
                compareArray(diffs, path, source, target);
            } else if (sourceType == NodeType.OBJECT && targetType == NodeType.OBJECT) {
                //both are json
                compareObjects(diffs, path, source, target);
            } else {
                //can be replaced

                diffs.add(Diff2.generateDiff(op.REPLACE, path, target));
            }
        }
    }

    private static void compareArray(List<Diff2> diffs, List<Object> path, JsonElement source, JsonElement target) {
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
                    diffs.add(Diff2.generateDiff(op.ADD, currPath, targetNode));
                    pos++;
                    targetIdx++;
                } else if (lcsNode.equals(targetNode)) { //targetNode node is same as lcs, but not src
                    //removal,
                    List<Object> currPath = getPath(path, pos);
                    diffs.add(Diff2.generateDiff(op.REMOVE, currPath, srcNode));
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

    private static Integer removeRemaining(List<Diff2> diffs, List<Object> path, int pos, int srcIdx, int srcSize, JsonElement source) {

        while (srcIdx < srcSize) {
            List<Object> currPath = getPath(path, pos);
            diffs.add(Diff2.generateDiff(op.REMOVE, currPath, source.get(srcIdx)));
            srcIdx++;
        }
        return pos;
    }

    private static Integer addRemaining(List<Diff2> diffs, List<Object> path, JsonElement target, int pos, int targetIdx, int targetSize) {
        while (targetIdx < targetSize) {
            JsonElement jsonNode = target.get(targetIdx);
            List<Object> currPath = getPath(path, pos);
            diffs.add(Diff2.generateDiff(op.ADD, currPath, jsonNode.deepCopy()));
            pos++;
            targetIdx++;
        }
        return pos;
    }

    private static void compareObjects(List<Diff2> diffs, List<Object> path, JsonElement source, JsonElement target) {
        Iterator<String> keysFromSrc = source.fieldNames();
        while (keysFromSrc.hasNext()) {
            String key = keysFromSrc.next();
            if (!target.has(key)) {
                //remove case
                List<Object> currPath = getPath(path, key);
                diffs.add(Diff2.generateDiff(op.REMOVE, currPath, source.get(key)));
                continue;
            }
            List<Object> currPath = getPath(path, key);
            generateDiffs(diffs, currPath, source.get(key), target.get(key));
        }
        Iterator<String> keysFromTarget = target.fieldNames();
        while (keysFromTarget.hasNext()) {
            String key = keysFromTarget.next();
            if (!source.has(key)) {
                //add case
                List<Object> currPath = getPath(path, key);
                diffs.add(Diff2.generateDiff(op.ADD, currPath, target.get(key)));
            }
        }
    }

    private static List<Object> getPath(List<Object> path, Object key) {
        List<Object> toReturn = new ArrayList<Object>();
        toReturn.addAll(path);
        toReturn.add(key);
        return toReturn;
    }

    private static List<JsonElement> getLCS(final JsonElement first, final JsonElement second) {

        Preconditions.checkArgument(first.isArray(), "LCS can only work on JSON arrays");
        Preconditions.checkArgument(second.isArray(), "LCS can only work on JSON arrays");

        return ListUtils.longestCommonSubsequence(Lists.newArrayList(first), Lists.newArrayList(second));
    }
}
