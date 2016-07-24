package com.beyondeye.zjsonpatch;

import java.io.InputStream;

/**
 * Created by daely on 7/23/2016.
 */
public class IOUtils {
    //see http://stackoverflow.com/questions/309424/read-convert-an-inputstream-to-a-string
    static String toString(InputStream is, String charsetName) {
        java.util.Scanner s = new java.util.Scanner(is, charsetName).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}

