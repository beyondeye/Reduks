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

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;

/**
 * User: gopi.vishwakarma
 * Date: 31/07/14
 */
public final class JsonPatch2 {
    static Operations op= new Operations();
    static Constants  consts = new Constants();

    private static final DecodePathFunction DECODE_PATH_FUNCTION = new DecodePathFunction();

    private JsonPatch2() {}

    private final static class DecodePathFunction implements Function<String, String> {
        @Override
        public String apply(String path) {
            return path.replaceAll("~1", "/").replaceAll("~0", "~"); // see http://tools.ietf.org/html/rfc6901#section-4
        }
    }

    private static JsonElement getPatchAttr(JsonObject jsonNode, String attr) {
        JsonElement child = jsonNode.get(attr);
        if (child == null)
            throw new InvalidJsonPatchException("Invalid JSON Patch payload (missing '" + attr + "' field)");
        return child;
    }

    private static JsonElement getPatchAttrWithDefault(JsonObject jsonNode, String attr, JsonElement defaultValue) {
        JsonElement child = jsonNode.get(attr);
        if (child == null)
            return defaultValue;
        else
            return child;
    }

    private static void process(JsonElement patch, JsonPatchProcessor processor, EnumSet<CompatibilityFlags> flags)
            throws InvalidJsonPatchException {

        if (!patch.isJsonArray())
            throw new InvalidJsonPatchException("Invalid JSON Patch payload (not an array)");
        Iterator<JsonElement> operations = patch.getAsJsonArray().iterator();
        while (operations.hasNext()) {
            JsonElement jsonNode = operations.next();
            if (!jsonNode.isJsonObject()) throw new InvalidJsonPatchException("Invalid JSON Patch payload (not an object)");
            int operation = op.opFromName(getPatchAttr(jsonNode.getAsJsonObject(), consts.OP).toString().replaceAll("\"", ""));
            List<String> path = getPath(getPatchAttr(jsonNode, consts.PATH));

            switch (operation) {
                case op.REMOVE: {
                    processor.remove(path);
                    break;
                }

                case op.ADD: {
                    JsonElement value;
                    if (!flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS))
                        value = getPatchAttr(jsonNode, consts.VALUE);
                    else
                        value = getPatchAttrWithDefault(jsonNode, consts.VALUE, NullNode.getInstance());
                    processor.add(path, value);
                    break;
                }

                case op.REPLACE: {
                    JsonElement value;
                    if (!flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS))
                        value = getPatchAttr(jsonNode, consts.VALUE);
                    else
                        value = getPatchAttrWithDefault(jsonNode, consts.VALUE, NullNode.getInstance());
                    processor.replace(path, value);
                    break;
                }

                case op.MOVE: {
                    List<String> fromPath = getPath(getPatchAttr(jsonNode, consts.FROM));
                    processor.move(fromPath, path);
                    break;
                }

                case op.COPY: {
                    List<String> fromPath = getPath(getPatchAttr(jsonNode, consts.FROM));
                    processor.copy(fromPath, path);
                    break;
                }

                case op.TEST: {
                    JsonElement value;
                    if (!flags.contains(CompatibilityFlags.MISSING_VALUES_AS_NULLS))
                        value = getPatchAttr(jsonNode, consts.VALUE);
                    else
                        value = getPatchAttrWithDefault(jsonNode, consts.VALUE, NullNode.getInstance());
                    processor.test(path, value);
                    break;
                }
            }
        }
    }

    public static void validate(JsonElement patch, EnumSet<CompatibilityFlags> flags) throws InvalidJsonPatchException {
        process(patch, NoopProcessor.INSTANCE, flags);
    }

    public static void validate(JsonElement patch) throws InvalidJsonPatchException {
        validate(patch, CompatibilityFlags.defaults());
    }

    public static JsonElement apply(JsonElement patch, JsonElement source, EnumSet<CompatibilityFlags> flags) throws JsonPatchApplicationException {
        ApplyProcessor processor = new ApplyProcessor(source);
        process(patch, processor, flags);
        return processor.result();
    }

    public static JsonElement apply(JsonElement patch, JsonElement source) throws JsonPatchApplicationException {
        return apply(patch, source, CompatibilityFlags.defaults());
    }

    private static List<String> getPath(JsonElement path) {
        List<String> paths = Splitter.on('/').splitToList(path.toString().replaceAll("\"", ""));
        return Lists.newArrayList(Iterables.transform(paths, DECODE_PATH_FUNCTION));
    }
}
