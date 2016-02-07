package com.example;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Lance on 2/5/2016.
 */
public class TsStackTest {

    public static void main(String[] args) {

        final int nThreads = 8;
        final int percPush = 50;

        ExecutorService es = Executors.newFixedThreadPool(nThreads);
        List<Callable<Void>> threads = new ArrayList<>();
        for(int i=0; i<nThreads; i++) {
            threads.add(new TsThread(percPush));
        }

        long start = System.nanoTime();
        try {
            es.invokeAll(threads);
        } catch (InterruptedException e) {}
        long end = System.nanoTime();

        System.out.println("nThreads: " + nThreads);
        System.out.print("push: " + percPush + "%");
        System.out.println("  pop: " + (100 - percPush) + "%");
        System.out.println("Elapsed time: " + (end - start) + " ns");

//        System.out.println("Creating SpBuffer");
//
//        SpBuffer spBuffer = new SpBuffer();
//        for(int i = 0; i < 10; i++) {
//            spBuffer.insSp(new TimestampedItem(i));
//        }
//        NodePair nodePair = spBuffer.getSp();
//        spBuffer.tryRemSP(nodePair.oldTop, nodePair.result);
//        spBuffer.insSp(new TimestampedItem(16));
//        spBuffer.insSp(new TimestampedItem(17));
//        spBuffer.insSp(new TimestampedItem(18));
//        nodePair = spBuffer.getSp();
//        spBuffer.tryRemSP(nodePair.oldTop, nodePair.result);
//
//        System.out.println("Printing SpBuffer");
//
//        spBuffer.printSpBuffer();
    }
}
