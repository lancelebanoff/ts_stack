package com.example;

/**
 * Created by Lance on 2/5/2016.
 */
public class TsStackTest {

    public static void main(String[] args) {
        System.out.println("Creating SpBuffer");

        SpBuffer spBuffer = new SpBuffer();
        for(int i = 0; i < 10; i++) {
            spBuffer.insSp(i);
        }
        NodePair nodePair = spBuffer.getSp();
        spBuffer.tryRemSP(nodePair.oldTop, nodePair.result);
        spBuffer.insSp(16);
        spBuffer.insSp(17);
        spBuffer.insSp(18);
        nodePair = spBuffer.getSp();
        spBuffer.tryRemSP(nodePair.oldTop, nodePair.result);
        for(int i = 0; i < 10; i++) {
            nodePair = spBuffer.getSp();
            spBuffer.tryRemSP(nodePair.oldTop, nodePair.result);
        }

        System.out.println("Printing SpBuffer");

        spBuffer.printSpBuffer();
    }
}
