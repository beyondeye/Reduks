package com.beyondeye.reduks.pcollections;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Created by daely on 5/21/2017.
 */

public class PMapSerializationTest extends TestCase {
    public void testIntTreePMapSerialization() throws IOException, ClassNotFoundException {

        IntTreePMap<String> src=IntTreePMap.empty();
        src = src.plus(1,"A");
        src = src.plus(2,"B");
        src = src.plus(3,"C");

        byte[] yourBytes = null;
        yourBytes = writeObjToBytes(src);
        IntTreePMap<String> dst= (IntTreePMap<String>) readObjFromBytes(yourBytes);

        assertEquals(src.size(),dst.size());
        for (Map.Entry<Integer, String> srcEntry:src.entrySet()) {
            String dstValue=dst.get(srcEntry.getKey());
            assertEquals(dstValue,srcEntry.getValue());
        }
    }
    public void testHashTreePMapSerialization() throws IOException, ClassNotFoundException {

        PMap<String,String> src=HashTreePMap.empty();
        src = src.plus("a","A");
        src = src.plus("b","B");
        src = src.plus("c","C");


        byte[] yourBytes = null;
        yourBytes = writeObjToBytes(src);
        PMap<String, String> dst= (PMap<String, String>) readObjFromBytes(yourBytes);

        assertEquals(src.size(),dst.size());
        for (Map.Entry<String, String> srcEntry:src.entrySet()) {
            String dstValue=dst.get(srcEntry.getKey());
            assertEquals(dstValue,srcEntry.getValue());
        }
    }

    private Object readObjFromBytes(byte[] yourBytes) throws IOException, ClassNotFoundException {
        Object o = null;
        ByteArrayInputStream bis = new ByteArrayInputStream(yourBytes);
        ObjectInput in = null;
        try {
            in = new ObjectInputStream(bis);
            o =  in.readObject();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return o;
    }

    private byte[] writeObjToBytes(Object src) throws IOException {
        byte[] yourBytes;ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = null;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(src);
            out.flush();
           yourBytes = bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                // ignore close exception
            }
        }
        return yourBytes;
    }
}
