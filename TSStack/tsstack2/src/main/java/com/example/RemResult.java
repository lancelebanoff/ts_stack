package com.example;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * Represents the result of an attempt to remove an item from the stack
 */
public class RemResult {

    public TimestampedItem item;
    public Result result;

    public static enum Result {
        VALID, INVALID, EMPTY
    }

    public RemResult(TimestampedItem item) {
        this.item = item;
        result = Result.VALID;
    }

    public RemResult(Result result) {
        this.result = result;
    }
}
