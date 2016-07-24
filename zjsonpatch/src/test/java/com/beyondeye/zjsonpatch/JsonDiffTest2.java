package com.beyondeye.zjsonpatch;

import com.beyondeye.zjsonpatch.utils.GsonObjectMapper;
import com.beyondeye.zjsonpatch.utils.IOUtils;
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
        InputStream resourceAsStream = JsonDiffTest.class.getResourceAsStream(path);
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
