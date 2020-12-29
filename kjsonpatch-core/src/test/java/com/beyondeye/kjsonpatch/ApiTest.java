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

package com.beyondeye.kjsonpatch;

import com.beyondeye.kjsonpatch.utils.GsonObjectMapper;
import com.google.gson.JsonElement;

import org.junit.Test;

import java.io.IOException;

/**
 * User: holograph
 * Date: 03/08/16
 */
public class ApiTest {
    @Test(expected = InvalidJsonPatchException.class)
    public void applyingNonArrayPatchShouldThrowAnException() throws IOException {
        GsonObjectMapper objectMapper = new GsonObjectMapper();
        JsonElement invalid = objectMapper.readTree("[{\"not\": \"a patch\"}]");
        JsonElement to = objectMapper.readTree("{\"a\":1}");
        JsonPatch.apply(invalid.getAsJsonArray(), to);
    }

    @Test(expected = InvalidJsonPatchException.class)
    public void applyingAnInvalidArrayShouldThrowAnException() throws IOException {
        GsonObjectMapper objectMapper = new GsonObjectMapper();
        JsonElement invalid = objectMapper.readTree("[1, 2, 3, 4, 5]");
        JsonElement to = objectMapper.readTree("{\"a\":1}");
        JsonPatch.apply(invalid.getAsJsonArray(), to);
    }

    @Test(expected = InvalidJsonPatchException.class)
    public void applyingAPatchWithAnInvalidOperationShouldThrowAnException() throws IOException {
        GsonObjectMapper objectMapper = new GsonObjectMapper();
        JsonElement invalid = objectMapper.readTree("[{\"op\": \"what\"}]");
        JsonElement to = objectMapper.readTree("{\"a\":1}");
        JsonPatch.apply(invalid.getAsJsonArray(), to);
    }

    @Test(expected = InvalidJsonPatchException.class)
    public void validatingNonArrayPatchShouldThrowAnException() throws IOException {
        GsonObjectMapper objectMapper = new GsonObjectMapper();
        JsonElement invalid = objectMapper.readTree("{\"not\": \"a patch\"}");
        JsonPatch.validate(invalid);
    }

    @Test(expected = InvalidJsonPatchException.class)
    public void validatingAnInvalidArrayShouldThrowAnException() throws IOException {
        GsonObjectMapper objectMapper = new GsonObjectMapper();
        JsonElement invalid = objectMapper.readTree("[1, 2, 3, 4, 5]");
        JsonPatch.validate(invalid);
    }

    @Test(expected = InvalidJsonPatchException.class)
    public void validatingAPatchWithAnInvalidOperationShouldThrowAnException() throws IOException {
        GsonObjectMapper objectMapper = new GsonObjectMapper();
        JsonElement invalid = objectMapper.readTree("[{\"op\": \"what\"}]");
        JsonPatch.validate(invalid);
    }
}
