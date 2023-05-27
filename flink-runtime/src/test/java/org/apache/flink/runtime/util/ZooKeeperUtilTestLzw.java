package org.apache.flink.runtime.util;

public class ZooKeeperUtilTestLzw {
    public static void main(String[] args) {
        String result = ZooKeeperUtils.generateZookeeperPath("root", "lzw");
        System.out.println(result);
    }
}
