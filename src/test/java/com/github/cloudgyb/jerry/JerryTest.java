package com.github.cloudgyb.jerry;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Unit test for simple App.
 */
public class JerryTest
        extends TestCase {
    /**
     * Create the test case
     *
     */
    public void test() {
        Map<String, String> stringStringHashMap = new ConcurrentHashMap<>();
        stringStringHashMap.put("a", "b");
        stringStringHashMap.put("c", "d");
        stringStringHashMap.put("e", "f");
        Set<String> strings = stringStringHashMap.keySet();
        for (String string : strings) {
            stringStringHashMap.remove(string);
        }
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp() {
        assertTrue(true);
    }
}
