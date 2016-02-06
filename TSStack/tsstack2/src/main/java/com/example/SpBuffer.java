package com.example;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class SpBuffer {

    Node top;

    public SpBuffer() {
        Node sentinel = new Node(null, true);
        sentinel.next = sentinel;
        top = sentinel;
    }

    public void insSp(Object item) {
        Node newNode = new Node(item, false);
        Node topMost = top;
        while(topMost.next != topMost && topMost.taken.get()) {
            topMost = topMost.next;
        }
        newNode.next = topMost;
        top = newNode;
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
            synchronized (top) {
                AtomicReference<Node> topRef = new AtomicReference<>(top);
                topRef.compareAndSet(oldTop, node);
            }
            return true;
        }
        return false;
    }

    public void printSpBuffer() {
        Node node = top;
        while(node.taken.get()) {
            node = node.next;
        }
        while(node.next != node) {
            System.out.print(node.item + ", ");
            node = node.next;
        }
    }
}


class Node {
    Node next;
    Object item;
    AtomicBoolean taken;

    public Node(Object item, boolean taken) {
        this.item = item;
        this.taken = new AtomicBoolean(taken);
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