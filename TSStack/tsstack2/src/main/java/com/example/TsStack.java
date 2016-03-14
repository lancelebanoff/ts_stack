package com.example;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * A concurrent timestamped stack. Each thread operating on the stack has a single producer, multiple
 * consumer buffer, which together comprise the stack.
 */
public class TsStack {
    LinkedList<SpBuffer> spBuffers = new LinkedList<>();

    public TsThread[] tsThreads;
    private int nThreads;
    private int numOrderings;
    private byte[][] spBufferOrderings;
    private AtomicInteger orderIdx;

    public TsStack(int nThreads) {
        this.nThreads = nThreads;
        numOrderings = factorial(nThreads);
        orderIdx = new AtomicInteger(0);
        tsThreads = new TsThread[nThreads];
        createSpBufferOrderings();
    }

    //When iterating through the single producer buffers, we will use a cycle of predetermined orderings rather than picking a random
    //ordering each time. Since we use a 2-D array of bytes, we assume that there will be no more than 15 threads.
    private void createSpBufferOrderings() {
        spBufferOrderings = new byte[numOrderings][nThreads];
        permute(0, new boolean[nThreads], new byte[nThreads]);
        shuffleOrderings();
    }

    private static int factorial(int n) {
        int fact = 1;
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

    private void permute(int p, boolean[] used, byte[] perm) {
        if(p == nThreads) {
            int idx = orderIdx.getAndIncrement() % numOrderings;
            for(int i=0; i<nThreads; i++) {
                spBufferOrderings[idx][i] = perm[i];
            }
        }
        for(byte i=0; i<nThreads; i++) {
            if(!used[i]) {
                used[i] = true;
                perm[p] = i;
                permute(p+1, used, perm);
                used[i] = false;
            }
        }
    }

    private void shuffleOrderings() {
        int idx;
        byte[] temp;
        Random random = new Random();
        for(int i = spBufferOrderings.length - 1; i > 0; i--) {
            idx = random.nextInt(i + 1);
            temp = spBufferOrderings[idx];
            spBufferOrderings[idx] = spBufferOrderings[i];
            spBufferOrderings[i] = temp;
        }
    }
    //Inserts an item into the stack
    TimestampedItem ins(Object item, SpBuffer spBuffer) {
        TimestampedItem tsItem = new TimestampedItem(item);
        spBuffer.insSp(tsItem);
        return tsItem;
    }

    //Attempts to remove an item from the top of the stack
    RemResult tryRem(long startTime) {

        while (true) {

            //Get the thread that is executing this method
            int threadID = TsThread.ThreadID.get();
            TsThread thread = tsThreads[threadID];

            NodePair youngestNodePair = null;
            TimestampedItem youngestItem = null;
            SpBuffer buffer = null;

            int sameCount = 0;

            //Look through each single-producer buffer by using one of the predetermined orderings
            //  byte[] order = spBufferOrderings[orderIdx.getAndUpdate(orderIdxOperator)];
            int idx = orderIdx.getAndIncrement();
            if (idx > numOrderings - 1) {
                idx = 0;
                orderIdx.set(0);
            }
            byte[] order = spBufferOrderings[idx];

            for (int i : order) {

                SpBuffer spBuffer = spBuffers.get(i);

                //This section keeps track of the top pointers of all spBuffers
                Node prevSeenTop = null;
                if (spBuffer.getId() > -1)
                    prevSeenTop = thread.topPointers[spBuffer.getId()];
                Node top = spBuffer.top;
                if (prevSeenTop != null && prevSeenTop == top)
                    sameCount++;
                thread.topPointers[spBuffer.getId()] = top;

                NodePair nodePair = spBuffer.getSp();
                if (nodePair.result == null) //The spBuffer was empty
                    continue;
                TimestampedItem item = nodePair.result.item;
                if (item.interval[1] > startTime) {
                    //The item was inserted after this thread started looking for an item to remove, so try to remove the item
                    if (spBuffer.tryRemSP(nodePair.oldTop, nodePair.result)) {
                        return new RemResult(item);
                    }
                    else {
                        continue;
                    }
                }
                //If the item was inserted more recently than the known youngest item, mark this item as youngest
                if (youngestItem == null || item.interval[0] > youngestItem.interval[1]) {
                    youngestNodePair = nodePair;
                    youngestItem = item;
                    buffer = spBuffer;
                }
            }

            //Try to remove the youngest item
            if(buffer != null) {
                if (buffer.tryRemSP(youngestNodePair.oldTop, youngestNodePair.result)) {
                    return new RemResult(youngestItem);
                }
            }
            //If the top pointers of all spBuffers have not changed since the last time this thread attempted to remove an item,
            //we know the stack is empty
            else if (sameCount == tsThreads.length) {
                return new RemResult(RemResult.Result.EMPTY);
            }
        }
    }
}
