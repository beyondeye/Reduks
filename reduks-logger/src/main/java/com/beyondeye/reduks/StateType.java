package com.beyondeye.reduks;

import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

/**
 * use gson facilities to extract type info from generic type
 * Created by daely on 7/25/2016.
 */
public class StateType<S> {
    public Type type= new TypeToken<S>(){}.getType();
}
