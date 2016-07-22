package com.beyondeye.reduks.logger.zjsonpatch

//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.node.ArrayNode;
//import com.fasterxml.jackson.databind.node.JsonNodeFactory;
//import com.fasterxml.jackson.databind.node.ObjectNode;
//import com.google.common.base.Function;
//import com.google.common.base.Joiner;
//import com.google.common.base.Preconditions;
//import com.google.common.collect.Iterables;
//import com.google.common.collect.Lists;
import com.beyondeye.reduks.logger.zjsonpatch.lcs.ListUtils
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.publicDeepCopy

//import org.apache.commons.collections4.ListUtils;

import org.json.JSONArray

import java.util.*

/**
 * User: gopi.vishwakarma
 * Date: 30/07/14
 */
object JsonDiff {


    fun asJson(source: JsonElement, target: JsonElement): JsonArray {
        val diffs = ArrayList<Diff>()
        val path = LinkedList<Any>()
        /**
         * generating diffs in the order of their occurrence
         */
        generateDiffs(diffs, path, source, target)
        /**
         * Merging remove & add to move operation
         */
        compactDiffs(diffs)

        return getJsonNodes(diffs)
    }

    /**
     * This method merge 2 diffs ( remove then add, or vice versa ) with same value into one Move operation,
     * all the core logic resides here only
     */
    private fun compactDiffs(diffs: MutableList<Diff>) {
        for (i in diffs.indices) {
            val diff1 = diffs[i]

            // if not remove OR add, move to next diff
            if (!(Operation.REMOVE == diff1.operation || Operation.ADD == diff1.operation)) {
                continue
            }

            for (j in i + 1..diffs.size - 1) {
                val diff2 = diffs[j]
                if (diff1.value != diff2.value) {
                    continue
                }

                var moveDiff: Diff? = null
                if (Operation.REMOVE == diff1.operation && Operation.ADD == diff2.operation) {
                    computeRelativePath(diff2.path, i + 1, j - 1, diffs)
                    moveDiff = Diff(Operation.MOVE, diff1.path, diff2.value, diff2.path)

                } else if (Operation.ADD == diff1.operation && Operation.REMOVE == diff2.operation) {
                    computeRelativePath(diff2.path, i, j - 1, diffs) // diff1's add should also be considered
                    moveDiff = Diff(Operation.MOVE, diff2.path, diff1.value, diff1.path)
                }
                if (moveDiff != null) {
                    diffs.removeAt(j)
                    diffs[i] = moveDiff
                    break
                }
            }
        }
    }

    //Note : only to be used for arrays
    //Finds the longest common Ancestor ending at Array
    private fun computeRelativePath(path: MutableList<Any>, startIdx: Int, endIdx: Int, diffs: List<Diff>) {
        val counters = ArrayList<Int>()

        resetCounters(counters, path.size)

        for (i in startIdx..endIdx) {
            val diff = diffs[i]
            //Adjust relative path according to #ADD and #Remove
            if (Operation.ADD == diff.operation || Operation.REMOVE == diff.operation) {
                updatePath(path, diff, counters)
            }
        }
        updatePathWithCounters(counters, path)
    }

    private fun resetCounters(counters: MutableList<Int>, size: Int) {
        for (i in 0..size - 1) {
            counters.add(0)
        }
    }

    private fun updatePathWithCounters(counters: List<Int>, path: MutableList<Any>) {
        for (i in counters.indices) {
            val value = counters[i]
            if (value != 0) {
                val currValue = Integer.parseInt(path[i].toString())
                path[i] = (currValue + value).toString()
            }
        }
    }

    //find longest common prefix of both the paths
    private fun updatePath(path: List<Any>, pseudo: Diff, counters: MutableList<Int>) {

        if (pseudo.path.size <= path.size) {
            var idx = -1
            for (i in 0..pseudo.path.size - 1 - 1) {
                if (pseudo.path[i] == path[i]) {
                    idx = i
                } else {
                    break
                }
            }
            if (idx == pseudo.path.size - 2) {
                if (pseudo.path[pseudo.path.size - 1] is Int) {
                    updateCounters(pseudo, pseudo.path.size - 1, counters)
                }
            }
        }
    }

    private fun updateCounters(pseudo: Diff, idx: Int, counters: MutableList<Int>) {
        if (Operation.ADD == pseudo.operation) {
            counters[idx] = counters[idx] - 1
        } else {
            if (Operation.REMOVE == pseudo.operation) {
                counters[idx] = counters[idx] + 1
            }
        }
    }

    private fun getJsonNodes(diffs: List<Diff>): JsonArray {
        //        JsonNodeFactory FACTORY = JsonNodeFactory.instance;
        val patch = JsonArray()
        for (diff in diffs) {
            val jsonNode = getJsonNode(diff)
            patch.add(jsonNode)
        }
        return patch
    }

    private fun getJsonNode(diff: Diff): JsonObject {
        val jsonNode = JsonObject()
        jsonNode.addProperty(Constants.OP, diff.operation.rfcName())
        jsonNode.addProperty(Constants.PATH, getArrayNodeRepresentation(diff.path))
        if (Operation.MOVE == diff.operation) {
            jsonNode.addProperty(Constants.FROM, getArrayNodeRepresentation(diff.path)) //required {from} only in case of Move Operation
            jsonNode.addProperty(Constants.PATH, getArrayNodeRepresentation(diff.toPath))  // destination Path
        }
        if (Operation.REMOVE != diff.operation && Operation.MOVE != diff.operation) { // setting only for Non-Remove operation
            jsonNode.add(Constants.VALUE, diff.value)
        }
        return jsonNode
    }

    fun EncodePath(`object`: Any): String {
        val path = `object`.toString() // see http://tools.ietf.org/html/rfc6901#section-4
        return path.replace("~".toRegex(), "~0").replace("/".toRegex(), "~1")
    }

    //join path parts in argument 'path', inserting a '/' between joined elements, starting with '/' and transforming the element of the list with ENCODE_PATH_FUNCTION
    private fun getArrayNodeRepresentation(path: List<Any>): String {
        //        return Joiner.on('/').appendTo(new StringBuilder().append('/'),
        //                Iterables.transform(path, ENCODE_PATH_FUNCTION)).toString();
        val sb = StringBuilder()
        for (i in path.indices) {
            sb.append('/')
            sb.append(EncodePath(path[i]))

        }
        return sb.toString()
    }

    private fun generateDiffs(diffs: MutableList<Diff>, path: List<Any>, source: JsonElement, target: JsonElement) {
        if (source != target) {
            val sourceType = NodeType.getNodeType(source)
            val targetType = NodeType.getNodeType(target)

            if (sourceType == NodeType.ARRAY && targetType == NodeType.ARRAY) {
                //both are arrays
                compareArray(diffs, path, source, target)
            } else if (sourceType == NodeType.OBJECT && targetType == NodeType.OBJECT) {
                //both are json
                compareObjects(diffs, path, source.asJsonObject, target.asJsonObject)
            } else {
                //can be replaced

                diffs.add(Diff.generateDiff(Operation.REPLACE, path, target))
            }
        }
    }

    private fun compareArray(diffs: MutableList<Diff>, path: List<Any>, source_: JsonElement, target_: JsonElement) {
        val source = source_.asJsonArray
        val target = target_.asJsonArray
        val lcs = getLCS(source, target)
        var srcIdx = 0
        var targetIdx = 0
        var lcsIdx = 0
        val srcSize = source.size()
        val targetSize = target.size()
        val lcsSize = lcs.size

        var pos = 0
        while (lcsIdx < lcsSize) {
            val lcsNode = lcs[lcsIdx]
            val srcNode = source.get(srcIdx)
            val targetNode = target.get(targetIdx)


            if (lcsNode == srcNode && lcsNode == targetNode) { // Both are same as lcs node, nothing to do here
                srcIdx++
                targetIdx++
                lcsIdx++
                pos++
            } else {
                if (lcsNode == srcNode) { // src node is same as lcs, but not targetNode
                    //addition
                    val currPath = getPath(path, pos)
                    diffs.add(Diff.generateDiff(Operation.ADD, currPath, targetNode))
                    pos++
                    targetIdx++
                } else if (lcsNode == targetNode) { //targetNode node is same as lcs, but not src
                    //removal,
                    val currPath = getPath(path, pos)
                    diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, srcNode))
                    srcIdx++
                } else {
                    val currPath = getPath(path, pos)
                    //both are unequal to lcs node
                    generateDiffs(diffs, currPath, srcNode, targetNode)
                    srcIdx++
                    targetIdx++
                    pos++
                }
            }
        }

        while (srcIdx < srcSize && targetIdx < targetSize) {
            val srcNode = source.get(srcIdx)
            val targetNode = target.get(targetIdx)
            val currPath = getPath(path, pos)
            generateDiffs(diffs, currPath, srcNode, targetNode)
            srcIdx++
            targetIdx++
            pos++
        }
        pos = addRemaining(diffs, path, target, pos, targetIdx, targetSize)
        removeRemaining(diffs, path, pos, srcIdx, srcSize, source)
    }

    private fun removeRemaining(diffs: MutableList<Diff>, path: List<Any>, pos: Int, srcIdx: Int, srcSize: Int, source_: JsonElement): Int {
        var srcIdx = srcIdx
        val source = source_.asJsonArray
        while (srcIdx < srcSize) {
            val currPath = getPath(path, pos)
            diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.get(srcIdx)))
            srcIdx++
        }
        return pos
    }

    private fun addRemaining(diffs: MutableList<Diff>, path: List<Any>, target_: JsonElement, pos: Int, targetIdx: Int, targetSize: Int): Int {
        var pos = pos
        var targetIdx = targetIdx
        val target = target_.asJsonArray
        while (targetIdx < targetSize) {
            val jsonNode = target.get(targetIdx)
            val currPath = getPath(path, pos)
            diffs.add(Diff.generateDiff(Operation.ADD, currPath, jsonNode.publicDeepCopy()))
            pos++
            targetIdx++
        }
        return pos
    }

    private fun compareObjects(diffs: MutableList<Diff>, path: List<Any>, source: JsonObject, target: JsonObject) {
        val keysFromSrc = source.entrySet().iterator()
        while (keysFromSrc.hasNext()) {
            val key = keysFromSrc.next().key
            if (!target.has(key)) {
                //remove case
                val currPath = getPath(path, key)
                diffs.add(Diff.generateDiff(Operation.REMOVE, currPath, source.get(key)))
                continue
            }
            val currPath = getPath(path, key)
            generateDiffs(diffs, currPath, source.get(key), target.get(key))
        }
        val keysFromTarget = target.entrySet().iterator()
        while (keysFromTarget.hasNext()) {
            val key = keysFromTarget.next().key
            if (!source.has(key)) {
                //add case
                val currPath = getPath(path, key)
                diffs.add(Diff.generateDiff(Operation.ADD, currPath, target.get(key)))
            }
        }
    }

    private fun getPath(path: List<Any>, key: Any): List<Any> {
        val toReturn = ArrayList<Any>()
        toReturn.addAll(path)
        toReturn.add(key)
        return toReturn
    }

    private fun getLCS(first_: JsonElement, second_: JsonElement): List<JsonElement> {
        if (!first_.isJsonArray) throw IllegalArgumentException("LCS can only work on JSON arrays")
        if (!second_.isJsonArray) throw IllegalArgumentException("LCS can only work on JSON arrays")
        val first = first_ as JsonArray
        val second = second_ as JsonArray
        return ListUtils.longestCommonSubsequence(first.toList(),second.toList())
    }
}
