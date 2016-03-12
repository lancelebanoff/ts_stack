package com.example;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * This class defines a thread that can operate on the TsStack
 */
public class TsThread implements Callable<Void> {

    SpBuffer mBuffer;
    int percPush; //percentage of operations that should be push operations (remainder are pops)
    int id;
    //topPointers[n] is this thread's last observation of the top pointer of the spBuffer with id = n
    int[] topPointers;
    int nBuffers;
    int nOps;
    TsStack tsStack;

    public TsThread(int percPush, int nBuffers, int nOps, TsStack tsStack) {
        this.percPush = percPush;
        topPointers = new int[nBuffers];
        Arrays.fill(topPointers, -2);
        this.nBuffers = nBuffers;
        mBuffer = new SpBuffer();
        tsStack.spBuffers.add(mBuffer);
        this.nOps = nOps;
        this.tsStack = tsStack;
    }

    //Inserts an item into the stack
    public void ins(int val) {
        tsStack.ins(val, mBuffer);
    }

    //TODO: Remove later
    public void doInsert() {
        ins(TsStackTest.idx.getAndIncrement());
    }

    @Override
    public Void call() throws Exception {

        id = ThreadID.get();
        mBuffer.setId(id);
        tsStack.tsThreads[id] = this;

        Random rand = new Random();

        //Execute a push or pop on the stack. The frequency of push is controlled by percPush
        int i;
        for(i=0; i<nOps; i++) {
            int x = rand.nextInt(100) + 1;
            if(x <= percPush) {
                ins(TsStackTest.idx.getAndIncrement());
            }
            else {
                tsStack.tryRem(TsStack.getCurrentTime());
            }
        }
        TsStackTest.printDebug(id, "numOps executed = " + i);
        return null;
    }

    //This class is implemented as given in appendix A.2 in the textbook
    public static class ThreadID {
        private static volatile int nextID = 0;
        private static class ThreadLocalID extends ThreadLocal<Integer> {
            protected synchronized Integer initialValue() {
                return nextID++;
            }
        }
        private static ThreadLocalID threadID = new ThreadLocalID();
        public static int get() {
            return threadID.get();
        }
        public static void set(int index) {
            threadID.set(index);
        }
    }
}

