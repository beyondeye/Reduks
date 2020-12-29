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

package com.beyondeye.kjsonpatch


import com.google.gson.*

class ApplyProcessor(target: JsonElement) : JsonPatchProcessor {

    private var target: JsonElement

    init {
        this.target = target.deepCopy()
    }

    fun result(): JsonElement {
        return target
    }

    override fun move(fromPath: List<String>, toPath: List<String>) {
        val parentNode = getParentNode(fromPath)
        val field = fromPath[fromPath.size - 1].replace("\"".toRegex(), "")
        val valueNode = if (parentNode!!.isJsonArray) parentNode.asJsonArray.get(Integer.parseInt(field)) else parentNode.asJsonObject.get(field)
        remove(fromPath)
        add(toPath, valueNode)
    }

    override fun copy(fromPath: List<String>, toPath: List<String>) {
        val parentNode = getParentNode(fromPath)
        val field = fromPath[fromPath.size - 1].replace("\"".toRegex(), "")
        val valueNode = if (parentNode!!.isJsonArray) parentNode.asJsonArray.get(Integer.parseInt(field)) else parentNode.asJsonObject.get(field)
        add(toPath, valueNode)
    }

    override fun test(path: List<String>, value: JsonElement) {
        if (path.isEmpty()) {
            throw JsonPatchApplicationException("[TEST Operation] path is empty , path : ")
        } else {
            val parentNode = getParentNode(path)
            if (parentNode == null) {
                throw JsonPatchApplicationException("[TEST Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToReplace = path[path.size - 1].replace("\"".toRegex(), "")
                if (fieldToReplace == "" && path.size == 1)
                    target = value
                else if (!parentNode.isContainerNode())
                    throw JsonPatchApplicationException("[TEST Operation] parent is not a container in source, path provided : $path | node : $parentNode")
                else if (parentNode.isJsonArray) {
                    val target = parentNode.asJsonArray
                    val idxStr = path[path.size - 1]

                    if ("-" == idxStr) {
                        // see http://tools.ietf.org/html/rfc6902#section-4.1
                        if (target.get(target.size() - 1) != value) {
                            throw JsonPatchApplicationException("[TEST Operation] value mismatch")
                        }
                    } else {
                        val idx = arrayIndex(idxStr.replace("\"".toRegex(), ""), target.size())
                        if (target.get(idx) != value) {
                            throw JsonPatchApplicationException("[TEST Operation] value mismatch")
                        }
                    }
                } else {
                    val target = parentNode.asJsonObject
                    val key = path[path.size - 1].replace("\"".toRegex(), "")
                    if (target.get(key) != value) {
                        throw JsonPatchApplicationException("[TEST Operation] value mismatch")
                    }
                }
            }
        }
    }

    override fun add(path: List<String>, value: JsonElement) {
        if (path.isEmpty()) {
            throw JsonPatchApplicationException("[ADD Operation] path is empty , path : ")
        } else {
            val parentNode = getParentNode(path)
            if (parentNode == null) {
                throw JsonPatchApplicationException("[ADD Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToReplace = path[path.size - 1].replace("\"".toRegex(), "")
                if (fieldToReplace == "" && path.size == 1)
                    target = value
                else if (!parentNode.isContainerNode())
                    throw JsonPatchApplicationException("[ADD Operation] parent is not a container in source, path provided : $path | node : $parentNode")
                else if (parentNode.isJsonArray)
                    addToArray(path, value, parentNode)
                else
                    addToObject(path, parentNode, value)
            }
        }
    }

    private fun addToObject(path: List<String>, node: JsonElement, value: JsonElement) {
        val target = node.asJsonObject
        val key = path[path.size - 1].replace("\"".toRegex(), "")
        target.add(key, value)
    }

    private fun addToArray(path: List<String>, value: JsonElement, parentNode: JsonElement) {
        val target = parentNode.asJsonArray
        val idxStr = path[path.size - 1]

        if ("-" == idxStr) {
            // see http://tools.ietf.org/html/rfc6902#section-4.1
            target.add(value)
        } else {
            val idx = arrayIndex(idxStr.replace("\"".toRegex(), ""), target.size())
            target.insert(idx, value)
        }
    }

    override fun replace(path: List<String>, value: JsonElement) {
        if (path.isEmpty()) {
            throw JsonPatchApplicationException("[Replace Operation] path is empty")
        } else {
            val parentNode = getParentNode(path)
            if (parentNode == null) {
                throw JsonPatchApplicationException("[Replace Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToReplace = path[path.size - 1].replace("\"".toRegex(), "")
                if (fieldToReplace.isNullOrEmpty() && path.size == 1)
                    target = value
                else if (parentNode.isJsonObject)
                    parentNode.asJsonObject.add(fieldToReplace, value)
                else if (parentNode.isJsonArray) {
                    val parentNode_ = parentNode.asJsonArray
                    parentNode_.set(arrayIndex(fieldToReplace, parentNode_.size() - 1), value)
                } else
                    throw JsonPatchApplicationException("[Replace Operation] noSuchPath in source, path provided : " + path)
            }
        }
    }

    override fun remove(path: List<String>) {
        if (path.isEmpty()) {
            throw JsonPatchApplicationException("[Remove Operation] path is empty")
        } else {
            val parentNode = getParentNode(path)
            if (parentNode == null) {
                throw JsonPatchApplicationException("[Remove Operation] noSuchPath in source, path provided : " + path)
            } else {
                val fieldToRemove = path[path.size - 1].replace("\"".toRegex(), "")
                if (parentNode.isJsonObject)
                    parentNode.asJsonObject.remove(fieldToRemove)
                else if (parentNode.isJsonArray) {
                    val parentNode_ = parentNode.asJsonArray
                    parentNode_.remove(arrayIndex(fieldToRemove, parentNode_.size() - 1))
                } else
                    throw JsonPatchApplicationException("[Remove Operation] noSuchPath in source, path provided : " + path)
            }
        }
    }

    private fun getParentNode(fromPath: List<String>): JsonElement? {
        val pathToParent = fromPath.subList(0, fromPath.size - 1) // would never by out of bound, lets see
        return getNode(target, pathToParent, 1)
    }

    private fun getNode(ret: JsonElement, path: List<String>, pos_: Int): JsonElement? {
        var pos = pos_
        if (pos >= path.size) {
            return ret
        }
        val key = path[pos]
        if (ret.isJsonArray) {
            val keyInt = Integer.parseInt(key.replace("\"".toRegex(), ""))
            val element = ret.asJsonArray.get(keyInt)
            if (element == null)
                return null
            else
                return getNode(ret.asJsonArray.get(keyInt), path, ++pos)
        } else if (ret.isJsonObject) {
            val ret_ = ret.asJsonObject
            if (ret_.has(key)) {
                return getNode(ret_.get(key), path, ++pos)
            }
            return null
        } else {
            return ret
        }
    }

    private fun arrayIndex(s: String, max: Int): Int {
        val index = Integer.parseInt(s)
        if (index < 0) {
            throw JsonPatchApplicationException("index Out of bound, index is negative")
        } else if (index > max) {
            throw JsonPatchApplicationException("index Out of bound, index is greater than " + max)
        }
        return index
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

private fun  JsonArray.insert(index: Int, value_: JsonElement?):JsonArray {
    val value=value_ ?:JsonNull.INSTANCE
    if(index>=size()) {
        add(value)
        return this
    }
    if(index<0)
        insert(this,0,value)
    else
        insert(this,index,value)
    return this
}

private fun  JsonElement.isContainerNode(): Boolean {
    return this.isJsonArray || this.isJsonObject
}
