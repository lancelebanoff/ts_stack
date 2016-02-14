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

    public static AtomicInteger idx;
    public static boolean verbose = false;
//    public static boolean verbose = true;

    public static void main(String[] args) {

        final int[] nThreadsArray = {1,2,4,8};
        final int[] percPushArray = {1,25,50,75,99};
        final int nOps = 500000;

        for(int i = 0; i < nThreadsArray.length; i++) {
            for(int j = 0; j < percPushArray.length; j++) {

                int nThreads = nThreadsArray[i];
                int percPush = percPushArray[j];

                idx = new AtomicInteger(0);

                TsStack.getInstance().tsThreads = new TsThread[nThreads];

                ExecutorService es = Executors.newFixedThreadPool(nThreads);
                List<Callable<Void>> threadsToExecute = new ArrayList<>();
                for(int threadIndex = 0; threadIndex < nThreads; threadIndex++) {
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
                System.out.println();
            }
        }
    }

    public static void runSingleTest(int nThreads, int percPush, int nOps) {

        idx = new AtomicInteger(0);

        TsStack.getInstance().tsThreads = new TsThread[nThreads];

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
        System.out.println();
    }

    public static void printDebug(String s) {
        if(verbose)
            System.out.println(s);
    }
}
