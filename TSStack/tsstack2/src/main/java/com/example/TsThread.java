package com.example;

import java.util.Random;
import java.util.concurrent.Callable;

/**
 * Created by Kevin on 2/6/2016.
 */
public class TsThread implements Callable<Void> {

    SpBuffer mBuffer;
    int percPush; //percentage of operations that should be push operations (remainder are pops)

    public TsThread(int percPush) {
        this.percPush = percPush;
    }

    public void ins(Object item) {
        TsBuffer.getInstance().ins(item, mBuffer);
    }

    @Override
    public Void call() throws Exception {
        int N = 500000;
        Random rand = new Random();

        for(int i=0; i<N; i++) {
            int x = rand.nextInt(100) + 1;
            if(x <= percPush) {
                ins(x);
            }
            else {
                TsBuffer.getInstance().tryRem(System.currentTimeMillis());
            }
        }
        return null;
    }
}
