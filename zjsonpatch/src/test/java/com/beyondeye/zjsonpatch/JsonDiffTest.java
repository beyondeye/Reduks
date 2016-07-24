package com.beyondeye.zjsonpatch;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * Unit test
 */
public class JsonDiffTest {
    static GsonObjectMapper objectMapper = new GsonObjectMapper();
    static JsonArray jsonNode;

    @BeforeClass
    public static void beforeClass() throws IOException {
        String path = "/testdata/sample.json";
        InputStream resourceAsStream = JsonDiffTest.class.getResourceAsStream(path);
        String testData = IOUtils.toString(resourceAsStream, "UTF-8");
        jsonNode =  objectMapper.readTree(testData).getAsJsonArray();
    }

    @Test
    public void testSampleJsonDiff() throws Exception {
        for (int i = 0; i < jsonNode.size(); i++) {
            JsonElement first = jsonNode.get(i).getAsJsonObject().get("first");
            JsonElement second = jsonNode.get(i).getAsJsonObject().get("second");

            System.out.println("Test # " + i);
            System.out.println(first);
            System.out.println(second);

            JsonArray actualPatch = JsonDiff.asJson(first, second);


            System.out.println(actualPatch);

            JsonElement secondPrime = JsonPatch.apply(actualPatch, first);
            System.out.println(secondPrime);
            Assert.assertTrue(second.equals(secondPrime));
        }
    }

    @Test
    public void testGeneratedJsonDiff() throws Exception {
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            JsonElement first = TestDataGenerator.generate(random.nextInt(10));
            JsonElement second = TestDataGenerator.generate(random.nextInt(10));

            JsonArray actualPatch = JsonDiff.asJson(first, second);
            System.out.println("Test # " + i);

            System.out.println(first);
            System.out.println(second);
            System.out.println(actualPatch);

            JsonElement secondPrime = JsonPatch.apply(actualPatch, first);
            System.out.println(secondPrime);
            Assert.assertTrue(second.equals(secondPrime));
        }
    }
}
