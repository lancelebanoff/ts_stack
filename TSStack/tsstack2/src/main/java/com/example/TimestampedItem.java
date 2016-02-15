package com.example;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 */
public class TimestampedItem {
    long[] interval;
    Object data;

    public TimestampedItem(Object data) {
        interval = new long[2];
        interval[0] = System.nanoTime();
        interval[1] = System.nanoTime();
        this.data = data;
    }
}
