package com.github.cloudgyb.jerry;

import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author geng
 * @since 2025/02/19 10:26:56
 */
public class URLPatternTest extends TestCase {
    public void test0() {
        Pattern compile = Pattern.compile("^/test/.+\\.do$");
        Matcher matcher = compile.matcher("/test/.do");
        assertTrue(matcher.matches());
    }
}
