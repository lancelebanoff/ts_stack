package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Lance on 2/5/2016.
 */
public class TsStackTest {

    public static AtomicInteger idx = new AtomicInteger(0);
    public static boolean verbose = false;
//    public static boolean verbose = true;

    public static void main(String[] args) {

        final int nThreads = 2;
        final int percPush = 50;
        final int nOps = 100000;

        TsBuffer.getInstance().tsThreads = new TsThread[nThreads];

        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        List<Callable<Void>> threadsToExecute = new ArrayList<>();
        for(int i=0; i<nThreads; i++) {
            threadsToExecute.add(new TsThread(percPush, nThreads, nOps));
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
    }

    public static void printDebug(String s) {
        if(verbose)
            System.out.println(s);
    }
}
