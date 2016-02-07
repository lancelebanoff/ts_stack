package com.example;

import java.util.LinkedList;

/**
 * Created by Lance on 2/5/2016.
 */
public class TsBuffer {
    LinkedList<SpBuffer> spBuffers = new LinkedList<>();

    private static TsBuffer tsBuffer = new TsBuffer();
    private TsBuffer() {}

    Object ins(Object element) {
        return null;
    }

    public static TsBuffer getInstance() {
        return tsBuffer;
    }

    synchronized TimestampedItem ins(Object item, SpBuffer spBuffer) {
        TimestampedItem tsItem = new TimestampedItem(item);
        spBuffer.insSp(tsItem);
        return tsItem;
    }

    synchronized RemResult tryRem(long startTime) {
        NodePair youngestNodePair = null;
        TimestampedItem youngestItem = null;
        SpBuffer buffer = null;

        for(SpBuffer spBuffer : spBuffers) {
            NodePair nodePair = spBuffer.getSp();
            TimestampedItem item = nodePair.result.item;
            if(item.interval[1] > startTime) {
                if(spBuffer.tryRemSP(nodePair.oldTop, nodePair.result))
                    return new RemResult(item);
            }
            if(youngestItem == null || item.interval[0] > youngestItem.interval[1]) {
                youngestNodePair = nodePair;
                youngestItem = item;
                buffer = spBuffer;
            }
        }

        //TODO: Add emptyness check
        if(buffer != null && buffer.tryRemSP(youngestNodePair.oldTop, youngestNodePair.result))
            return new RemResult(youngestItem);
        return new RemResult(RemResult.Result.INVALID);
    }
}
