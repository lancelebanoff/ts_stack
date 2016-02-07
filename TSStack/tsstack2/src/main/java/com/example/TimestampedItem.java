package com.example;

/**
 * Created by Kevin on 2/6/2016.
 */
public class TimestampedItem {
    long[] interval;
    Object data;

    public TimestampedItem(Object data) {
        interval = new long[2];
        interval[0] = System.currentTimeMillis();
        interval[1] = System.currentTimeMillis();
        this.data = data;
    }
}
