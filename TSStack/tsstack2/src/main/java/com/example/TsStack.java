package com.example;

import java.util.LinkedList;

/**
 * Authors: Kevin Joslyn, Lance Lebanoff, and Logan Lebanoff
 *
 * A concurrent timestamped stack. Each thread operating on the stack has a single producer, multiple
 * consumer buffer, which together comprise the stack.
 */
public class TsStack {
    LinkedList<SpBuffer> spBuffers = new LinkedList<>();

    private static TsStack tsStack = new TsStack();
    public static TsThread[] tsThreads;

    private TsStack() {}

    public static TsStack getInstance() {
        return tsStack;
    }

    public static void clear() {
        tsStack = new TsStack();
        tsThreads = null;
    }

    //Inserts an item into the stack
    void ins(int val, SpBuffer spBuffer) {
        long info = createInfo();
        spBuffer.insSp(info, val);
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

        while(true) {
            //Get the thread that is executing this method
            int threadID = TsThread.ThreadID.get();
            TsThread thread = tsThreads[threadID];

            TsStackTest.printDebug(threadID, "Thread " + threadID + " trying to remove node");
            GetSpResult youngestResult = null;
            SpBuffer buffer = null;

            int sameCount = 0;

            //Look through each single-producer buffer
            for (SpBuffer spBuffer : spBuffers) {

                //This section keeps track of the top pointers of all spBuffers
                int prevSeenTop = -1;
                if (spBuffer.getId() > -1)
                    prevSeenTop = thread.topPointers[spBuffer.getId()];
                int top = spBuffer.getTop();
                if (prevSeenTop != -1 && prevSeenTop == top)
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
                //If the item was inserted more recently than the known youngest item, mark this item as youngest
                if (youngestResult == null || getSpResult.getStartOfInterval() > youngestResult.getEndOfInterval()) {
                    youngestResult = getSpResult;
                    buffer = spBuffer;
                }
            }

            if (buffer != null) {
                try {
                    int val = buffer.tryRemSP(youngestResult);
                    if (val != youngestResult.value) {
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
