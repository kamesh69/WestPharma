package com.westpharma.aisdet.utils;

import org.testng.asserts.SoftAssert;

public final class AssertUtils {

    private AssertUtils() {
    }

    public static void assertEquals(Object actual, Object expected, String message) {
        org.testng.Assert.assertEquals(actual, expected, message);
    }

    public static void assertTrue(boolean condition, String message) {
        org.testng.Assert.assertTrue(condition, message);
    }

    public static void assertFalse(boolean condition, String message) {
        org.testng.Assert.assertFalse(condition, message);
    }

    public static SoftAssert softAssert() {
        return new SoftAssert();
    }
}
