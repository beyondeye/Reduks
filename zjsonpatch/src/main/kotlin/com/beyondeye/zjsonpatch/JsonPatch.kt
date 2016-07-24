package com.beyondeye.zjsonpatch

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.publicDeepCopy

/**
 * User: gopi.vishwakarma
 * Date: 31/07/14

 * * use Gson instead of Jackson, converted to Kotlin and some clean up
 * Dario Elyasy 22/7/2016
 */
object JsonPatch {
    internal val op= Operations()
    internal val consts= Constants()
    @JvmStatic fun apply(patch: JsonArray, source: JsonElement): JsonElement {
        val operations = patch.iterator()
        var ret = source.publicDeepCopy()
        while (operations.hasNext()) {
            val jsonNode = operations.next() as JsonObject
            val operation = op.opFromName(jsonNode.get(consts.OP).toString().replace("\"".toRegex(), ""))
            val path = getPath(jsonNode.get(consts.PATH))
            var fromPath: List<String>? = null
            if (op.MOVE == operation) {
                fromPath = getPath(jsonNode.get(consts.FROM))
            }
            var value: JsonElement? = null
            if (op.REMOVE != operation && op.MOVE != operation) {
                value = jsonNode.get(consts.VALUE)
            }

            when (operation) {
                op.REMOVE -> remove(ret, path)
                op.REPLACE -> ret = replace(ret, path, value!!)
                op.ADD -> ret = add(ret, path, value!!)
                op.MOVE -> ret = move(ret, fromPath!!, path)
            }
        }
        return ret
    }

    private fun move(node: JsonElement, fromPath: List<String>, toPath: List<String>): JsonElement {
        val parentNode = getParentNode(node, fromPath)
        val field = fromPath[fromPath.size - 1].replace("\"".toRegex(), "")
        //        JsonElement valueNode =  parentNode.isJsonArray() ? parentNode.get(Integer.parseInt(field)) : parentNode.get(field);
        val valueNode: JsonElement
        if (parentNode!!.isJsonArray)
            valueNode = (parentNode as JsonArray).get(Integer.parseInt(field))
        else
            valueNode = (parentNode as JsonObject).get(field)
        remove(node, fromPath)
        return add(node, toPath, valueNode)
    }

    private fun add(node: JsonElement, path: List<String>, value: JsonElement): JsonElement {
        if (path.isEmpty()) {
            throw RuntimeException("[ADD Operation] path is empty , path : ")
        } else {
            val parentNode = getParentNode(node, path)
            if (parentNode == null) {
                throw RuntimeException("[ADD Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToReplace = path[path.size - 1].replace("\"".toRegex(), "")
                if (fieldToReplace == "" && path.size == 1) {
                    return value
                }
                if (!isContainerNode(parentNode))
                //not array or object node
                {
                    throw RuntimeException("[ADD Operation] parent is not a container in source, path provided : $path | node : $parentNode")
                } else {
                    if (parentNode.isJsonArray) {
                        addToArray(path, value, parentNode)
                    } else {
                        addToObject(path, parentNode, value)
                    }
                }
            }
        }
        return node
    }

    private fun isContainerNode(parentNode: JsonElement): Boolean {
        return parentNode.isJsonObject || parentNode.isJsonArray
    }

    private fun addToObject(path: List<String>, node: JsonElement, value: JsonElement) {
        val target = node as JsonObject
        val key = path[path.size - 1].replace("\"".toRegex(), "")
        target.add(key, value)
    }

    private fun addToArray(path: List<String>, value: JsonElement, parentNode: JsonElement) {
        val target = parentNode as JsonArray
        val idxStr = path[path.size - 1]

        if ("-" == idxStr) {
            // see http://tools.ietf.org/html/rfc6902#section-4.1
            target.add(value)
        } else {
            val idx = Integer.parseInt(idxStr.replace("\"".toRegex(), ""))
            if (idx < target.size()) {
                insert(target, idx, value)
            } else {
                if (idx === target.size()) {
                    target.add(value)
                } else {
                    throw RuntimeException("[ADD Operation] [addToArray] index Out of bound, index provided is higher than allowed, path " + path)
                }
            }
        }
    }

    //TODO insert not very efficient: find a better way to do it?
    private fun insert(target: JsonArray, idx: Int, value: JsonElement) {
        val lastidx = target.size() - 1
        val last = target.get(lastidx)
        target.add(last)
        //move up elements after idx
        for (i in (lastidx - 1) downTo idx)
            target.set(i + 1, target.get(i))
        //finally insert new value
        target.set(idx, value)
    }

    private fun replace(node: JsonElement, path: List<String>, value: JsonElement): JsonElement {
        if (path.isEmpty()) {
            throw RuntimeException("[Replace Operation] path is empty")
        } else {
            val parentNode = getParentNode(node, path)
            if (parentNode == null) {
                throw RuntimeException("[Replace Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToReplace = path[path.size - 1].replace("\"".toRegex(), "")
                if (isNullOrEmpty(fieldToReplace) && path.size == 1) {
                    return value
                }
                if (parentNode.isJsonObject)
                    (parentNode as JsonObject).add(fieldToReplace, value)
                else
                    (parentNode as JsonArray).set(Integer.parseInt(fieldToReplace), value)
            }
            return node
        }
    }

    private fun isNullOrEmpty(s: String?): Boolean {
        return s == null || s.length == 0
    }

    private fun remove(node: JsonElement, path: List<String>) {
        if (path.isEmpty()) {
            throw RuntimeException("[Remove Operation] path is empty")
        } else {
            val parentNode = getParentNode(node, path)
            if (parentNode == null) {
                throw RuntimeException("[Remove Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToRemove = path[path.size - 1].replace("\"".toRegex(), "")
                if (parentNode.isJsonObject)
                    (parentNode as JsonObject).remove(fieldToRemove)
                else
                    (parentNode as JsonArray).remove(Integer.parseInt(fieldToRemove))
            }
        }
    }

    private fun getParentNode(node: JsonElement, fromPath: List<String>): JsonElement? {
        val pathToParent = fromPath.subList(0, fromPath.size - 1) // would never by out of bound, lets see
        return getNode(node, pathToParent, 1)
    }

    private fun getNode(ret: JsonElement, path: List<String>, pos: Int): JsonElement? {
        var pos = pos
        if (pos >= path.size) {
            return ret
        }
        val key = path[pos]
        if (ret.isJsonArray) {
            val keyInt = Integer.parseInt(key.replace("\"".toRegex(), ""))
            return getNode((ret as JsonArray).get(keyInt), path, ++pos)
        } else if (ret.isJsonObject) {
            if ((ret as JsonObject).has(key)) {
                return getNode(ret.get(key), path, ++pos)
            }
            return null
        } else {
            return ret
        }
    }

    private fun decodePath(path: String): String {
        return path.replace("~1".toRegex(), "/").replace("~0".toRegex(), "~") // see http://tools.ietf.org/html/rfc6901#section-4
    }

    private fun getPath(path: JsonElement): List<String> {
        //        List<String> paths = Splitter.on('/').splitToList(path.toString().replaceAll("\"", ""));
        //        return Lists.newArrayList(Iterables.transform(paths, DECODE_PATH_FUNCTION));
        val pathstr = path.toString().replace("\"", "")
        val paths = pathstr.split("/")
        return paths.map { decodePath(it) }
    }
}
