package com.beyondeye.zjsonpatch;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.hamcrest.core.IsEqual.equalTo;

/**
 * @author ctranxuan (streamdata.io).
 */
public abstract class AbstractTest {
    static GsonObjectMapper objectMapper = new GsonObjectMapper();
    static JsonArray jsonNode;
    static JsonArray errorNode;

    protected AbstractTest(String fileName) throws IOException {
        String path = "/testdata/" + fileName + ".json";
        InputStream resourceAsStream = JsonDiffTest.class.getResourceAsStream(path);
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        JsonObject testsNode = objectMapper.readTree(testData).getAsJsonObject();
        jsonNode = testsNode.get("ops").getAsJsonArray();
        errorNode =testsNode.get("errors").getAsJsonArray();
    }

    @Test
    public void testPatchAppliedCleanly() throws Exception {
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonElement first = jsonNode.get(i).getAsJsonObject().get("node");
            JsonElement second = jsonNode.get(i).getAsJsonObject().get("expected");
            JsonArray patch = jsonNode.get(i).getAsJsonObject().get("op").getAsJsonArray();
            String message = jsonNode.get(i).getAsJsonObject().has("message") ? jsonNode.get(i).getAsJsonObject().get("message").toString() : "";

            System.out.println("Test # " + i);
            System.out.println(first);
            System.out.println(second);
            System.out.println(patch);

            JsonElement secondPrime = JsonPatch.apply(patch, first);
            System.out.println(secondPrime);
            Assert.assertThat(message, secondPrime, equalTo(second));
        }
    }

//    @Test
//    public void testPatchSyntax() throws Exception {
//        for (int i = 0; i < jsonNode.size(); i++) {
//            JsonNode first = jsonNode.get(i).get("node");
//            JsonNode second = jsonNode.get(i).get("expected");
//            JsonNode patch = jsonNode.get(i).get("op");
//
//            System.out.println("Test # " + i);
//            System.out.println(first);
//            System.out.println(second);
//            System.out.println(patch);
//
//            JsonNode diff = JsonDiff.asJson(first, second);
//            System.out.println(diff);
//            Assert.assertThat(diff, equalTo(patch));
//        }
//    }

    @Test(expected = RuntimeException.class)
    public void testErrorsAreCorrectlyReported() {
        for (int i = 0; i < errorNode.size(); i++) {
            JsonElement first = errorNode.get(i).getAsJsonObject().get("node");
            JsonArray patch = errorNode.get(i).getAsJsonObject().get("op").getAsJsonArray();

            System.out.println("Error Test # " + i);
            System.out.println(first);
            System.out.println(patch);

            JsonElement secondPrime = JsonPatch.apply(patch, first);
            System.out.println(secondPrime);
        }

        if (errorNode.size() == 0) {
            throw new RuntimeException("dummy exception");
        }
    }

}
