package com.beyondeye.kjsonpatch.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Created by daely on 7/23/2016.
 */
public class GsonObjectMapper {
    private JsonParser parser= new JsonParser();

    public JsonElement readTree(String jsondata) {
        return parser.parse(jsondata);
    }
}
