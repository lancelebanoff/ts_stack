package com.example;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
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
    private Random mRandom;

    public TsStack(int nThreads) {
        this.nThreads = nThreads;
        numOrderings = factorial(nThreads);
        orderIdx = new AtomicInteger(0);
        tsThreads = new TsThread[nThreads];
        createSpBufferOrderings();
        mRandom = new Random();
    }

    private void createSpBufferOrderings() {
        spBufferOrderings = new byte[numOrderings][nThreads];
        permute(0, new boolean[nThreads], new byte[nThreads]);
        shuffleOrderings();
    }

    private static int factorial(int n) {
        int fact = 1; // this  will be the result
        for (int i = 1; i <= n; i++) {
            fact *= i;
        }
        return fact;
    }

//    private IntUnaryOperator orderIdxOperator = new IntUnaryOperator() {
//        @Override
//        public int applyAsInt(int operand) {
//            return (operand + 1) % numOrderings;
//        }
//    };

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
    void ins(int val, SpBuffer spBuffer) {
        spBuffer.insSp(val);
    }

    //From upper to lower bits: 31 for start time, 31 for end time, 1 empty, 1 taken
    //Taken bit will be initialized to 0 so nothing needs to be done for that
    public static long createInfo() {
        long startTime = System.currentTimeMillis() << 33;
        long endTime = System.currentTimeMillis() << 33 >>> 31;
        return startTime + endTime;
    }

    public static long getCurrentTime() {
        return System.currentTimeMillis() << 33 >>> 33;
    }

    //Attempts to remove an item from the top of the stack
    RemResult tryRem(long startTime) {

//        int count = 0;
        while(true) {
//            count++;
//            if(count > 5)
//                System.out.println("Problem");

			// Get the thread that is executing this method
			int threadID = TsThread.ThreadID.get();
			TsThread thread = tsThreads[threadID];

			TsStackTest.printDebug(threadID, "trying to remove node");
			GetSpResult youngestResult = null;

            int sameCount = 0;

            List<GetSpResult> resultList = new ArrayList<>();
            //Look through each single-producer buffer

            byte[] order = spBufferOrderings[orderIdx.getAndIncrement() % numOrderings];

            for(int idx : order) {

                SpBuffer spBuffer = spBuffers.get(idx);

                //This section keeps track of the top pointers of all spBuffers
                int prevSeenTop = -2;
                if (spBuffer.getId() > -1)
                    prevSeenTop = thread.topPointers[spBuffer.getId()];
                int top = spBuffer.getTop();
                if (prevSeenTop != -2 && prevSeenTop == top)
                    sameCount++;
                thread.topPointers[spBuffer.getId()] = top;

                GetSpResult getSpResult = spBuffer.getSp();
                if (getSpResult.idx == -1) //The spBuffer was empty
                    continue;
                if (getSpResult.getEndOfInterval() > startTime) {
                    //The item was inserted after this thread started looking for an item to remove, so try to remove the item
                    try {
                        int val = spBuffer.tryRemSP(getSpResult);
                        TsStackTest.printDebug(threadID, "    Eliminated " + val + " with interval [" + getSpResult.getStartOfInterval() + ", " + getSpResult.getEndOfInterval() + "]");
                        return new RemResult(val);
                    } catch (SpBuffer.RemovalException e) {
                        //Item already removed, keep going
                        continue;
                    }
                }
//                //If the item was inserted more recently than the known youngest item, mark this item as youngest
                if (youngestResult == null || getSpResult.getEndOfInterval() > youngestResult.getEndOfInterval()) {
                    youngestResult = getSpResult;
                }
                resultList.add(getSpResult);
            }

            List<GetSpResult> newResultList = new ArrayList<>();
            for(GetSpResult result : resultList) {
                if(result.getEndOfInterval() >= youngestResult.getStartOfInterval())
                    newResultList.add(result);
            }
            GetSpResult chosenResult = null;
            if(newResultList.size() > 1) {
                chosenResult = newResultList.get(mRandom.nextInt(newResultList.size()));
            }
            else if(newResultList.size() == 1) {
                chosenResult = newResultList.get(0);
            }
            if(chosenResult != null) {
                SpBuffer buffer = spBuffers.get(chosenResult.spBufferIdx);
                try {
                    int val = buffer.tryRemSP(chosenResult);
                    if (val != chosenResult.value) {
                        throw new Exception("Error: value mismatch!");
                    }
                    TsStackTest.printDebug(threadID, "    Removed " + val + " with interval [" + youngestResult.getStartOfInterval() + ", " + youngestResult.getEndOfInterval() + "]");
                    return new RemResult(val);
                } catch (SpBuffer.RemovalException e) { //The item was already taken, try again
                    TsStackTest.printDebug(threadID, "INVALID!!!");
                    continue;
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    e.printStackTrace();
                    return null;
                }
            }
            //If the top pointers of all spBuffers have not changed since the last time this thread attempted to remove an item,
            //we know the stack is empty
            else if (sameCount == tsThreads.length) {
                TsStackTest.printDebug(threadID, "EMPTY!!!");
                return new RemResult(RemResult.Result.EMPTY);
            }
            //If buffer == null and sameCount != tsThreads.length, the stack may be empty but we have to try one more time to be sure. If the
            //stack is empty, then next time sameCount == tsThreads.length will be true.
        } //end while(true) loop
    }
}
