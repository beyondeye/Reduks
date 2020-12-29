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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author ctranxuan (streamdata.io).
 */
public class JsonDiffTest2 {
    static GsonObjectMapper objectMapper = new GsonObjectMapper();
    static JsonArray jsonNode;

    @BeforeClass
    public static void beforeClass() throws IOException {
        String path = "/testdata/diff.json";
        InputStream resourceAsStream = JsonDiffTest2.class.getResourceAsStream(path);
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        jsonNode = objectMapper.readTree(testData).getAsJsonArray();
    }

    @Test
    public void testPatchAppliedCleanly() throws Exception {
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonElement first = jsonNode.get(i).getAsJsonObject().get("first");
            JsonElement second = jsonNode.get(i).getAsJsonObject().get("second");
            JsonArray patch = jsonNode.get(i).getAsJsonObject().get("patch").getAsJsonArray();
            String message = jsonNode.get(i).getAsJsonObject().get("message").toString();

            System.out.println("Test # " + i);
            System.out.println(first);
            System.out.println(second);
            System.out.println(patch);

            JsonElement secondPrime = JsonPatch.apply(patch, first);
            System.out.println(secondPrime);
            Assert.assertThat(message, secondPrime, equalTo(second));
        }

    }
}
