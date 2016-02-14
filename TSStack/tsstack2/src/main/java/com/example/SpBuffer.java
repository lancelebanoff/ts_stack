package com.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SpBuffer {

    Node top;
    private int id;

    public SpBuffer() {
        id = -1;
        Node sentinel = new Node(null, true);
        sentinel.next = sentinel;
        top = sentinel;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getId() { return id; }

    public void insSp(TimestampedItem item) {
        Node newNode = new Node(item, false);
        Node topMost = top;
        while(topMost.next != topMost && topMost.taken.get()) {
            topMost = topMost.next;
        }
        newNode.next = topMost;
        top = newNode;
//        TsStackTest.printDebug("SpBuffer " + id + " after inserting node... " + toString());
    }

    public NodePair getSp() {
        Node oldTop = top;
        Node result = oldTop;
        while(true) {
            if(!result.taken.get()) {
                return new NodePair(result, oldTop);
            }
            else if(result.next == result) {
                return new NodePair(null, oldTop);
            }
            else {
                result = result.next;
            }
        }
    }

    public boolean tryRemSP(Node oldTop, Node node) {
        if(node.taken.compareAndSet(false, true)) {
            synchronized (this) {
                AtomicReference<Node> topRef = new AtomicReference<>(top);
                topRef.compareAndSet(oldTop, node);
            }
//            TsStackTest.printDebug("  SpBuffer " + id + " after removing node... " + toString());
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        String s = "";
        Node node = top;
        while(node.next != node) {
            s += node + ", ";
            node = node.next;
        }
        return s;
    }
}


class Node {
    Node next;
    TimestampedItem item;
    AtomicBoolean taken;

    public Node(TimestampedItem item, boolean taken) {
        this.item = item;
        this.taken = new AtomicBoolean(taken);
    }

    @Override
    public String toString() {
        String s = taken.get() ? "T" : "";
        return s + item.data.toString();
    }
}

class NodePair {
    Node result;
    Node oldTop;

    public NodePair(Node result, Node oldTop) {
        this.result = result;
        this.oldTop = oldTop;
    }
}