package com.beyondeye.kjsonpatch;

/*
 * Copyright 2016 flipkart.com kjsonpatch.
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

import com.beyondeye.kjsonpatch.utils.GsonObjectMapper;
import com.beyondeye.kjsonpatch.utils.IOUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PatchTestCase {

    private final boolean operation;
    private final JsonObject node;

    private PatchTestCase(boolean isOperation, JsonObject node) {
        this.operation = isOperation;
        this.node = node;
    }

    public boolean isOperation() {
        return operation;
    }

    public JsonObject getNode() {
        return node;
    }

    private static final GsonObjectMapper MAPPER = new GsonObjectMapper();

    public static Collection<PatchTestCase> load(String fileName) throws IOException {
        String path = "/testdata/" + fileName + ".json";
        InputStream resourceAsStream = PatchTestCase.class.getResourceAsStream(path);
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        JsonElement tree = MAPPER.readTree(testData);

        List<PatchTestCase> result = new ArrayList<PatchTestCase>();
        for (JsonElement node : tree.getAsJsonObject().get("errors").getAsJsonArray()) {
            if (isEnabled(node)) {
                result.add(new PatchTestCase(false, node.getAsJsonObject()));
            }
        }
        for (JsonElement node : tree.getAsJsonObject().get("ops").getAsJsonArray()) {
            if (isEnabled(node)) {
                result.add(new PatchTestCase(true, node.getAsJsonObject()));
            }
        }
        return result;
    }

    private static boolean isEnabled(JsonElement node) {
        JsonElement disabled = node.getAsJsonObject().get("disabled");
        return (disabled == null || !disabled.getAsBoolean());
    }
}
