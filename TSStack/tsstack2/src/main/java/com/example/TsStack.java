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
    synchronized TimestampedItem ins(Object item, SpBuffer spBuffer) {
        TimestampedItem tsItem = new TimestampedItem(item);
        spBuffer.insSp(tsItem);
        return tsItem;
    }

    //Attempts to remove an item from the top of the stack
    synchronized RemResult tryRem(long startTime) {

        //Get the thread that is executing this method
        int threadID = TsThread.ThreadID.get();
        TsThread thread = tsThreads[threadID];

        NodePair youngestNodePair = null;
        TimestampedItem youngestItem = null;
        SpBuffer buffer = null;

        int sameCount = 0;

        //Look through each single-producer buffer
        for(SpBuffer spBuffer : spBuffers) {

            //This section keeps track of the top pointers of all spBuffers
            Node prevSeenTop = null;
            if(spBuffer.getId() > -1)
                prevSeenTop = thread.topPointers[spBuffer.getId()];
            Node top = spBuffer.top;
            if(prevSeenTop != null && prevSeenTop == top)
                sameCount++;
            thread.topPointers[spBuffer.getId()] = top;

            NodePair nodePair = spBuffer.getSp();
            if(nodePair.result == null) //The spBuffer was empty
                continue;
            TimestampedItem item = nodePair.result.item;
            if(item.interval[1] > startTime) {
                //The item was inserted after this thread started looking for an item to remove, so try to remove the item
                if(spBuffer.tryRemSP(nodePair.oldTop, nodePair.result)) {
                    return new RemResult(item);
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
        if(buffer != null && buffer.tryRemSP(youngestNodePair.oldTop, youngestNodePair.result)) {
            return new RemResult(youngestItem);
        }
        //If the top pointers of all spBuffers have not changed since the last time this thread attempted to remove an item,
        //we know the stack is empty
        if(sameCount == tsThreads.length) {
            return new RemResult(RemResult.Result.EMPTY);
        }
        return new RemResult(RemResult.Result.INVALID);
    }
}
