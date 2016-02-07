package com.example;

/**
 * Created by Kevin on 2/6/2016.
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
