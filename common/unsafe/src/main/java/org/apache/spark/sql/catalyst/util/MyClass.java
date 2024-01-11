package org.apache.spark.sql.catalyst.util;

public class MyClass {

    static {
        System.load("/Users/stefan.kandic/spark/common/unsafe/src/main/java/org/apache/spark/sql/catalyst/util/libnative.dylib");
    }

    public static native long createCollator(String locale);
    public static native int compare(long collatorId, String s1, String s2);
    public static native int compare2(long collatorId, byte[] s1, byte[] s2);
}
