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


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.List;

class ApplyProcessor implements JsonPatchProcessor {

    private JsonElement target;

    ApplyProcessor(JsonElement target) {
        this.target = target.publicDeepCopy();
    }

    public JsonElement result() {
        return target;
    }

    @Override
    public void move(List<String> fromPath, List<String> toPath) {
        JsonElement parentNode = getParentNode(fromPath);
        String field = fromPath.get(fromPath.size() - 1).replaceAll("\"", "");
        JsonElement valueNode =  parentNode.isJsonArray() ? parentNode.getAsJsonArray().get(Integer.parseInt(field)) : parentNode.getAsJsonObject().get(field);
        remove(fromPath);
        add(toPath, valueNode);
    }

    @Override
    public void copy(List<String> fromPath, List<String> toPath) {
        JsonElement parentNode = getParentNode(fromPath);
        String field = fromPath.get(fromPath.size() - 1).replaceAll("\"", "");
        JsonElement valueNode =  parentNode.isJsonArray() ? parentNode.getAsJsonArray().get(Integer.parseInt(field)) : parentNode.getAsJsonObject().get(field);
        add(toPath, valueNode);
    }

    @Override
    public void test(List<String> path, JsonElement value) {
        if (path.isEmpty()) {
            throw new JsonPatchApplicationException("[TEST Operation] path is empty , path : ");
        } else {
            JsonElement parentNode = getParentNode(path);
            if (parentNode == null) {
                throw new JsonPatchApplicationException("[TEST Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToReplace = path.get(path.size() - 1).replaceAll("\"", "");
                if (fieldToReplace.equals("") && path.size() == 1)
                    target = value;
                else if (!parentNode.isContainerNode())
                    throw new JsonPatchApplicationException("[TEST Operation] parent is not a container in source, path provided : " + path + " | node : " + parentNode);
                else if (parentNode.isJsonArray()) {
                    final JsonArray target =  parentNode.getAsJsonArray();
                    String idxStr = path.get(path.size() - 1);

                    if ("-".equals(idxStr)) {
                        // see http://tools.ietf.org/html/rfc6902#section-4.1
                        if(!target.get(target.size()-1).equals(value)){
                            throw new JsonPatchApplicationException("[TEST Operation] value mismatch");
                        }
                    } else {
                        int idx = arrayIndex(idxStr.replaceAll("\"", ""), target.size());
                        if(!target.get(idx).equals(value)){
                            throw new JsonPatchApplicationException("[TEST Operation] value mismatch");
                        }
                    }
                }
                else {
                    final JsonObject target = parentNode.getAsJsonObject();
                    String key = path.get(path.size() - 1).replaceAll("\"", "");
                    if(!target.get(key).equals(value)){
                        throw new JsonPatchApplicationException("[TEST Operation] value mismatch");
                    }
                }
            }
        }
    }

    @Override
    public void add(List<String> path, JsonElement value) {
        if (path.isEmpty()) {
            throw new JsonPatchApplicationException("[ADD Operation] path is empty , path : ");
        } else {
            JsonElement parentNode = getParentNode(path);
            if (parentNode == null) {
                throw new JsonPatchApplicationException("[ADD Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToReplace = path.get(path.size() - 1).replaceAll("\"", "");
                if (fieldToReplace.equals("") && path.size() == 1)
                    target = value;
                else if (!parentNode.isContainerNode())
                    throw new JsonPatchApplicationException("[ADD Operation] parent is not a container in source, path provided : " + path + " | node : " + parentNode);
                else if (parentNode.isJsonArray())
                    addToArray(path, value, parentNode);
                else
                    addToObject(path, parentNode, value);
            }
        }
    }

    private void addToObject(List<String> path, JsonElement node, JsonElement value) {
        final JsonObject target =  node.getAsJsonObject();
        String key = path.get(path.size() - 1).replaceAll("\"", "");
        target.add(key, value);
    }

    private void addToArray(List<String> path, JsonElement value, JsonElement parentNode) {
        final JsonArray target =  parentNode.getAsJsonArray();
        String idxStr = path.get(path.size() - 1);

        if ("-".equals(idxStr)) {
            // see http://tools.ietf.org/html/rfc6902#section-4.1
            target.add(value);
        } else {
            int idx = arrayIndex(idxStr.replaceAll("\"", ""), target.size());
            target.insert(idx, value);
        }
    }

    @Override
    public void replace(List<String> path, JsonElement value) {
        if (path.isEmpty()) {
            throw new JsonPatchApplicationException("[Replace Operation] path is empty");
        } else {
            JsonElement parentNode = getParentNode(path);
            if (parentNode == null) {
                throw new JsonPatchApplicationException("[Replace Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToReplace = path.get(path.size() - 1).replaceAll("\"", "");
                if (Strings.isNullOrEmpty(fieldToReplace) && path.size() == 1)
                    target = value;
                else if (parentNode.isJsonObject())
                    parentNode.getAsJsonObject().add(fieldToReplace, value);
                else if (parentNode.isJsonArray()) {
                    JsonArray parentNode_=parentNode.getAsJsonArray();
                    parentNode_.set(arrayIndex(fieldToReplace, parentNode_.size() - 1), value);
                }
                else
                    throw new JsonPatchApplicationException("[Replace Operation] noSuchPath in source, path provided : " + path);
            }
        }
    }

    @Override
    public void remove(List<String> path) {
        if (path.isEmpty()) {
            throw new JsonPatchApplicationException("[Remove Operation] path is empty");
        } else {
            JsonElement parentNode = getParentNode(path);
            if (parentNode == null) {
                throw new JsonPatchApplicationException("[Remove Operation] noSuchPath in source, path provided : " + path);
            } else {
                String fieldToRemove = path.get(path.size() - 1).replaceAll("\"", "");
                if (parentNode.isJsonObject())
                    parentNode.getAsJsonObject().remove(fieldToRemove);
                else if (parentNode.isJsonArray()){
                    JsonArray parentNode_=parentNode.getAsJsonArray();
                    parentNode_.remove(arrayIndex(fieldToRemove, parentNode_.size() - 1));
                }

                else
                    throw new JsonPatchApplicationException("[Remove Operation] noSuchPath in source, path provided : " + path);
            }
        }
    }

    private JsonElement getParentNode(List<String> fromPath) {
        List<String> pathToParent = fromPath.subList(0, fromPath.size() - 1); // would never by out of bound, lets see
        return getNode(target, pathToParent, 1);
    }

    private JsonElement getNode(JsonElement ret, List<String> path, int pos) {
        if (pos >= path.size()) {
            return ret;
        }
        String key = path.get(pos);
        if (ret.isJsonArray()) {
            int keyInt = Integer.parseInt(key.replaceAll("\"", ""));
            JsonElement element = ret.getAsJsonArray().get(keyInt);
            if (element == null)
                return null;
            else
                return getNode(ret.getAsJsonArray().get(keyInt), path, ++pos);
        } else if (ret.isJsonObject()) {
            JsonObject ret_=ret.getAsJsonObject();
            if (ret_.has(key)) {
                return getNode(ret_.get(key), path, ++pos);
            }
            return null;
        } else {
            return ret;
        }
    }

    private int arrayIndex(String s, int max) {
        int index = Integer.parseInt(s);
        if (index < 0) {
            throw new JsonPatchApplicationException("index Out of bound, index is negative");
        } else if (index > max) {
            throw new JsonPatchApplicationException("index Out of bound, index is greater than " + max);
        }
        return index;
    }
}
