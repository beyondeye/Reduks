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
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.EnumSet;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class CompatibilityTest {

    GsonObjectMapper mapper;
    JsonElement addNodeWithMissingValue;
    JsonElement replaceNodeWithMissingValue;

    @Before
    public void setUp() throws Exception {
        mapper = new GsonObjectMapper();
        addNodeWithMissingValue = mapper.readTree("[{\"op\":\"add\",\"path\":\"a\"}]");
        replaceNodeWithMissingValue = mapper.readTree("[{\"op\":\"replace\",\"path\":\"a\"}]");
    }

    @Test
    public void withFlagAddShouldTreatMissingValuesAsNulls() throws IOException {
        JsonElement expected = mapper.readTree("{\"a\":null}");
        JsonElement result = JsonPatch.apply(addNodeWithMissingValue, new JsonObject(), EnumSet.of(CompatibilityFlags.MISSING_VALUES_AS_NULLS));
        assertThat(result, equalTo(expected));
    }

    @Test
    public void withFlagAddNodeWithMissingValueShouldValidateCorrectly() {
        JsonPatch.validate(addNodeWithMissingValue, EnumSet.of(CompatibilityFlags.MISSING_VALUES_AS_NULLS));
    }

    @Test
    public void withFlagReplaceShouldTreatMissingValuesAsNull() throws IOException {
        JsonElement source = mapper.readTree("{\"a\":\"test\"}");
        JsonElement expected = mapper.readTree("{\"a\":null}");
        JsonElement result = JsonPatch.apply(replaceNodeWithMissingValue, source, EnumSet.of(CompatibilityFlags.MISSING_VALUES_AS_NULLS));
        assertThat(result, equalTo(expected));
    }

    @Test
    public void withFlagReplaceNodeWithMissingValueShouldValidateCorrectly() {
        JsonPatch.validate(addNodeWithMissingValue, EnumSet.of(CompatibilityFlags.MISSING_VALUES_AS_NULLS));
    }
}
