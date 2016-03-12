package com.example;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * Creates a stack and spawns threads to perform push and pop operations.
 */
public class TsStackTest {

    public static AtomicInteger idx;
    //    public static boolean verbose = false;
    public static boolean verbose = true;

    public static void main(String[] args) {
        ArrayList<Long[]> list;
        final int nThreads = Integer.parseInt(args[0]);
        final int percPush = Integer.parseInt(args[1]);
        final int nOps = Integer.parseInt(args[2]);

        SpBuffer.setMaxNumElements(nOps);

        idx = new AtomicInteger(0);

        TsStack.getInstance().tsThreads = new TsThread[nThreads];

//        TsThread thread = new TsThread(percPush, nThreads, nOps, TsStack.getInstance());
//        thread.doInsert();
        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        List<Callable<Void>> threadsToExecute = new ArrayList<>();
        for(int i=0; i<nThreads; i++) {
            threadsToExecute.add(new TsThread(percPush, nThreads, nOps, TsStack.getInstance()));
        }

        long start = System.currentTimeMillis();
        try {
            es.invokeAll(threadsToExecute);
        } catch (InterruptedException e) {}
        es.shutdown();
        long end = System.currentTimeMillis();

        System.out.println("nThreads: " + nThreads);
        System.out.print("push: " + percPush + "%");
        System.out.println("  pop: " + (100 - percPush) + "%");
        System.out.println("Elapsed time: " + (end - start) + " ms");
        System.out.println();
    }
    public static void printDebug(int threadID, String s) {
        if(verbose) {
            System.out.println(threadID + ": " + s);
        }
    }
}

