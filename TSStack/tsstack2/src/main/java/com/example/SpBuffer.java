package com.example;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * SpBuffer is a single producer, multiple consumer array.
 * Each thread is a producer for one SpBuffer, and all threads are consumers.
 */
public class SpBuffer {

    private AtomicInteger top;
    private AtomicLongArray infoArray;
    int[] values;
    static int maxNumElements;
    private int id;

    public SpBuffer() {

        infoArray = new AtomicLongArray(maxNumElements);
        values = new int[maxNumElements];
        top = new AtomicInteger(-1);
    }

    public static void setMaxNumElements(int max) {
        maxNumElements = max;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    public int getTop() {
        return top.get();
    }

    public void doInsert(int val) {
        insSp(val);
        System.out.println(toString());
    }

    public int doTryRemSP() {
        int val = -1;
        try {
            val = tryRemSP(getSp());
            System.out.println(toString());
        } catch(Exception e) { e.printStackTrace(); }
        return val;
    }

    //From upper to lower bits: 31 for start time, 31 for end time, 1 empty, 1 taken
    //Taken bit will be initialized to 0 so nothing needs to be done for that
    public static long createInfo() {
        long startTime = System.currentTimeMillis() << 33;
        long endTime = System.currentTimeMillis() << 33 >>> 31;
        return startTime + endTime;
    }

    public void insSp(int value) {

        long startTime = System.currentTimeMillis() << 33;
        int idx = top.get();
        while(idx >= 0 && isTaken(idx)) {
            idx--;
        }
        int newTop = idx + 1;
        top.set(newTop);
        long endTime = System.currentTimeMillis() << 33 >>> 31;
        long info = startTime + endTime;

        infoArray.set(newTop, info);
        values[newTop] = value;
//        TsStackTest.printDebug(TsThread.ThreadID.get(), "SpBuffer " + id + " after inserting node... " + toString()
//        		+ " [" + (info >>> 33) + ", " + (info << 31 >>> 33) + "]");
    }

    public boolean isTaken(int idx) {

        long val = infoArray.get(idx);
        return (val & 1) == 1;
    }

    public GetSpResult getSp() {
        int oldTop = top.get();
        int idx = oldTop;
        while(true) {
            if(idx < 0) {
                //this buffer is empty
                return new GetSpResult(-1, -1, -1, -1);
            }
            else if(!isTaken(idx)) {
                //We found an item in the buffer that has not yet been taken
                GetSpResult getSpResult = new GetSpResult(infoArray.get(idx), values[idx], idx, oldTop);
                return getSpResult;
            }
            else {
                idx--;
            }
        }
    }

    class RemovalException extends Exception {
        RemoveErrorType errorType;
        public RemovalException(RemoveErrorType errorType) {
            this.errorType = errorType;
        }
    }

    enum RemoveErrorType {
        ALREADY_TAKEN
    }

    public int tryRemSP(GetSpResult getSpResult) throws RemovalException {
        if(infoArray.compareAndSet(getSpResult.idx, getSpResult.info, getSpResult.info + 1)) {
            top.compareAndSet(getSpResult.oldTop, getSpResult.idx);
//            TsStackTest.printDebug(TsThread.ThreadID.get(), "  SpBuffer " + id + " after removing node... " + toString());
            return getSpResult.value;
        }
        else
            throw new RemovalException(RemoveErrorType.ALREADY_TAKEN);
    }

    @Override
    public String toString() {
        String s = "";
        int idx = 0;
        while(idx <= top.get()) {
            if(isTaken(idx))
                s += "T";
            s += values[idx] + ", ";
            idx++;
        }
        return s;
    }
}

