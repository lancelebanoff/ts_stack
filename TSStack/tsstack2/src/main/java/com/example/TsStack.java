package com.example;

import java.util.LinkedList;

/**
 * Created by Lance on 2/5/2016.
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

    synchronized TimestampedItem ins(Object item, SpBuffer spBuffer) {
        TimestampedItem tsItem = new TimestampedItem(item);
        spBuffer.insSp(tsItem);
        return tsItem;
    }

    synchronized RemResult tryRem(long startTime) {

        int threadID = TsThread.ThreadID.get();
        TsThread thread = tsThreads[threadID];

//        TsStackTest.printDebug("Thread " + threadID + " trying to remove node");
        NodePair youngestNodePair = null;
        TimestampedItem youngestItem = null;
        SpBuffer buffer = null;

        int sameCount = 0;

        for(SpBuffer spBuffer : spBuffers) {

            Node prevSeenTop = null;
            if(spBuffer.getId() > -1)
                prevSeenTop = thread.topPointers[spBuffer.getId()];
            Node top = spBuffer.top;
            if(prevSeenTop != null && prevSeenTop == top)
                sameCount++;
            thread.topPointers[spBuffer.getId()] = top;

            NodePair nodePair = spBuffer.getSp();
            if(nodePair.result == null) //Empty stack
                continue;
            TimestampedItem item = nodePair.result.item;
            if(item.interval[1] > startTime) {
                if(spBuffer.tryRemSP(nodePair.oldTop, nodePair.result)) {
//                    TsStackTest.printDebug("    Eliminated " + item.data + " with interval [" + item.interval[0] + ", " + item.interval[1] + "]");
                    return new RemResult(item);
                }
            }
            if (youngestItem == null || item.interval[0] > youngestItem.interval[1]) {
                youngestNodePair = nodePair;
                youngestItem = item;
                buffer = spBuffer;
            }
        }

        if(buffer != null && buffer.tryRemSP(youngestNodePair.oldTop, youngestNodePair.result)) {
//            TsStackTest.printDebug("    Removed " + youngestItem.data + " with interval [" + youngestItem.interval[0] + ", " + youngestItem.interval[1] + "]");
            return new RemResult(youngestItem);
        }
        if(sameCount == tsThreads.length) {
//            TsStackTest.printDebug("EMPTY!!!");
            return new RemResult(RemResult.Result.EMPTY);
        }
//        TsStackTest.printDebug("INVALID!!!");
        return new RemResult(RemResult.Result.INVALID);
    }
}
