package com.example;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * Represents the result of an attempt to remove an item from the stack
 */
public class RemResult {

    public int val;
    public Result result;

    public static enum Result {
        VALID, EMPTY
    }

    public RemResult(int val) {
        this.val = val;
        result = Result.VALID;
    }

    public RemResult(Result result) {
        this.result = result;
    }
}

class GetSpResult {
    long info;
    int value;
    int idx;
    int oldTop;
    int spBufferIdx;
    public GetSpResult(int spBufferIdx, long info, int value, int idx, int oldTop) {
        this.spBufferIdx = spBufferIdx;
        this.info = info;
        this.value = value;
        this.idx = idx;
        this.oldTop = oldTop;
    }

    public long getStartOfInterval() {
        return info >>> 33;
    }

    public long getEndOfInterval() {
        return info << 31 >>> 33;
    }
}


