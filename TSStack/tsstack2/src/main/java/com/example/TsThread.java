package com.example;

import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Kevin on 2/6/2016.
 */
public class TsThread implements Callable<Void> {

    SpBuffer mBuffer;
    int percPush; //percentage of operations that should be push operations (remainder are pops)
    int id;
    Node[] topPointers;
    int nBuffers;
    int nOps;

    public TsThread(int percPush, int nBuffers, int nOps) {
        this.percPush = percPush;
        topPointers = new Node[nBuffers];
        this.nBuffers = nBuffers;
        mBuffer = new SpBuffer();
        TsBuffer.getInstance().spBuffers.add(mBuffer);
        this.nOps = nOps;
    }

    public void ins(Object item) {
        TsBuffer.getInstance().ins(item, mBuffer);
    }

    @Override
    public Void call() throws Exception {

        id = ThreadID.get();
        mBuffer.setId(id);
        TsBuffer.getInstance().tsThreads[id] = this;

        Random rand = new Random();

        for(int i=0; i<nOps; i++) {
            int x = rand.nextInt(100) + 1;
            if(x <= percPush) {
                ins(TsStackTest.idx.getAndIncrement());
            }
            else {
                TsBuffer.getInstance().tryRem(System.nanoTime());
            }
        }
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
